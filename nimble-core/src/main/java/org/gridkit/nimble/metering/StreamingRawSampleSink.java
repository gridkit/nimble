package org.gridkit.nimble.metering;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.ssh.OutputStreamRemoteAdapter;

public class StreamingRawSampleSink implements RawSampleSink, Serializable {

	private static final long serialVersionUID = 20140118L;

	private static final int BACK_REFS_LIMIT = 1 << 10;
	
	private Master master = new SinkMaster();
	private boolean compress;
	private transient RawSampleSink sink; 
	private transient List<ActiveStream> streams = new ArrayList<ActiveStream>();
	private transient boolean finished = false;
	private transient ObjectOutputStream slaveSink;
	private transient Set<Object> streamBackRefs;
	
	public StreamingRawSampleSink(RawSampleSink sink) {
		this(sink, true);
	}

	public StreamingRawSampleSink(RawSampleSink sink, boolean compress) {
		this.sink = sink;
		this.compress = compress;
	}
	
	@Override
	public synchronized void push(List<Map<Object, Object>> rows) {
		try {
			if (slaveSink == null) {
				OutputStream raw = master.getStream(String.valueOf(System.getProperty("node.name")));
				raw = new BufferedOutputStream(raw, 64 << 10);
				if (compress) {
					raw = new GZIPOutputStream(raw);
				}
				slaveSink = new ObjectOutputStream(raw);
				streamBackRefs = new HashSet<Object>();
			}
			for(Map<Object, Object> row: rows) {
				pushRow(row);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	private synchronized void pushRow(Map<Object, Object> row) throws IOException {
		StreamSampleRow ssr = new StreamSampleRow(row);
		StreamSampleRow.writeToStream(slaveSink, ssr, streamBackRefs);
		if (streamBackRefs.size() > BACK_REFS_LIMIT) {
			// have to reset stream periodically
			// otherwise it would be impossible 
			// to read due to huge back reference table
			slaveSink.reset();
			streamBackRefs.clear();
		}
	}
	
	@Override
	public synchronized void done() {
		try {
			if (slaveSink != null) {
				slaveSink.writeObject(null);
				slaveSink.close();
				slaveSink = null;
				streamBackRefs.clear();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void finish() {
		try {
			if (!finished) {
				finished = true;
				for(ActiveStream stream: streams) {
					stream.done.get();
				}
				sink.done();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}
	
	private synchronized OutputStream newInboundStream(String nodename) {
		if (finished) {
			throw new IllegalStateException("Streaming is finished");
		}
		StreamPipe pipe = new StreamPipe(1 << 20);
		InputStream is  = pipe.getInputStream();
		
		ActiveStream as = new ActiveStream(is);
		as.setName("SampleStreamer-" + nodename);
		streams.add(as);
		as.start();
		
		return new OutputStreamRemoteAdapter(pipe.getOutputStream());
	}
	
	private class SinkMaster implements Master {

		@Override
		public OutputStream getStream(String nodeName) {			
			return newInboundStream(nodeName);
		}
	}
	
	private class ActiveStream extends Thread {
		
		private ObjectInputStream is;
		private InputStream raw;
		private List<Map<Object, Object>> buffer = new ArrayList<Map<Object, Object>>();
		private int bufferLimit = 32;
		private FutureBox<Void> done = new FutureBox<Void>();

		public ActiveStream(InputStream is) {
			this.raw = is;
		}

		@Override
		public void run() {
			try {
				if (compress) {
					raw = new GZIPInputStream(raw);
				}
				is = new ObjectInputStream(raw);
				
				while(true) {
					StreamSampleRow sample = (StreamSampleRow) is.readUnshared();
					if (sample == null) {
						break;
					}
					buffer.add(sample.data);
					mayPush();
				}
				if (!buffer.isEmpty()) {
					sink.push(buffer);
				}
				buffer.clear();
				done.setData(null);
			}
			catch(Error e) {
				done.setError(e);				
				throw e;
			}
			catch(IOException e) {
				done.setError(e);				
				throw new RuntimeException(e);
			}
			catch(RuntimeException e) {
				done.setError(e);				
				throw e;
			} catch (ClassNotFoundException e) {
				done.setError(e);				
				throw new RuntimeException(e);
			}
		}
		
		private void mayPush() {
			if (buffer.size() >= bufferLimit) {
				sink.push(buffer);
				buffer.clear();
			}
		}
	}
	
	private static interface Master extends Remote {
		
		public OutputStream getStream(String nodename);
		
	}	
}
