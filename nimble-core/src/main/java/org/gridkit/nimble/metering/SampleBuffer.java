package org.gridkit.nimble.metering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.pivot.SampleAccumulator;

public class SampleBuffer implements SampleAccumulator {

	private File file;
	private ObjectOutputStream writeStream;
	private long sampleCount;
	
	public SampleBuffer() throws IOException {
		this(File.createTempFile("nimble", ".buf"));
	}

	public SampleBuffer(File swapFile) throws IOException {
		this.file = swapFile;
		FileOutputStream fos = new FileOutputStream(swapFile);
		writeStream = new ObjectOutputStream(fos);
		this.file.deleteOnExit();
	}

	@Override
	public void accumulate(SampleReader samples) {
		if (samples.isReady() || samples.next()) {
			while(true) {
				pushSample(samples);
				if (!samples.next()) {
					break;
				}
			}
		}
	}
	
	public synchronized void fsync() throws IOException {
		writeStream.flush();
	}

	private void pushSample(SampleReader samples) {
		Map<Object, Object> row = new HashMap<Object, Object>();
		for(Object key: samples.keySet()) {
			row.put(key, samples.get(key));
		}
		pushRow(row);
	}

	protected synchronized void pushRow(Map<Object, Object> row) {
		try {
			writeStream.writeUnshared(row);
			++sampleCount;
			if ((sampleCount % 32 << 10) == 0) {
				// have to reset stream periodically
				// otherwise it would be impossible 
				// to read due to huge back reference table
				writeStream.reset();
			}
		} catch (IOException e) {
			// TODO logging
			throw new RuntimeException(e);
		}
	}

	@Override
	public void flush() {
		try {
			fsync();
		} catch (IOException e) {
			// ignore
		}		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void feed(RawSampleSink sink, int batchSize) throws IOException {
		long limit;
		synchronized(this) {
			fsync();
			limit = sampleCount;
		}
		
		InputStream is = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(is);
		int readCounter = 0;
		List<Map<Object, Object>> rows = new ArrayList<Map<Object,Object>>(batchSize);
		while(readCounter < limit) {
			try {
				((List)rows).add(ois.readUnshared());
				++readCounter;
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
			if (rows.size() == batchSize) {
				sink.push(rows);
				rows.clear();
			}
		}
		if (rows.size() > 0) {
			sink.push(rows);
		}
		sink.done();
	}
	
	public RawSampleSink asSampleSink() {
		return new RawSampleSink() {

			@Override
			public void push(List<Map<Object, Object>> rows) {
				for(Map<Object, Object> row: rows) {
					pushRow(row);
				}
			}

			@Override
			public void done() {
				flush();
			}
		};
	}
	
	
	public interface RemoteSampleSink extends RawSampleSink, Remote {		
	}	
}
