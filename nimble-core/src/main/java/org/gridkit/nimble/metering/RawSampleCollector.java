package org.gridkit.nimble.metering;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.nimble.print.LinePrinter;

public class RawSampleCollector implements SampleBuffer.RemoteSampleSink, LinePrinter {

	private Set<String> header = new LinkedHashSet<String>();
	private SampleBuffer buffer;
	
	public RawSampleCollector() throws IOException {
		buffer = new SampleBuffer();
	}
	
	public RawSampleCollector(File file) throws IOException {
		buffer = new SampleBuffer(file);
	}
	
	@Override
	public synchronized void push(List<Map<Object, Object>> rows) {
		for(Map<Object, Object> row: rows) {
			for(Object key: row.keySet()) {
				header.add(key.toString());
			}
		}
		buffer.asSampleSink().push(rows);
	}

	@Override
	public synchronized void done() {
		// do nothing
	}

	@Override
	public void print(final Context context) {
		for(String h: header) {
			context.cell(h, "");
		}
		try {
			buffer.feed(new RawSampleSink() {
				
				@Override
				public void push(List<Map<Object, Object>> rows) {
					for(Map<Object, Object> row: rows) {
						for(Object key: row.keySet()) {
							context.cell(key.toString(), row.get(key));
						}
						context.newline();
					}				
				}
				
				@Override
				public void done() {
				}
			}, 100);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public synchronized void writeCsv(final Writer writer) throws IOException {
		for(String h: header) {
			writer.append(h).append(';');
		}
		writer.append('\n');
		try {
			buffer.feed(new RawSampleSink() {
				
				@Override
				public void push(List<Map<Object, Object>> rows) {
					try {
						Map<String, Object> tl = new HashMap<String, Object>();
						for(Map<Object, Object> row: rows) {
							for(Object key: row.keySet()) {
								tl.put(key.toString(), row.get(key));
							}
							for(String h: header) {
								Object v = tl.get(h);
								writer.append(v == null ? "" : String.valueOf(v)).append(';');
							}
							writer.append('\n');
							tl.clear();
						}
						System.gc();
						writer.flush();
					} catch (IOException e) {
						throw new IOExceptionWrapper(e);
					}
				}
				
				@Override
				public void done() {
				}
				
			}, 1024);
		}
		catch(IOExceptionWrapper e) {
			e.rethrow();
		}
		writer.flush();
	}
	
	@SuppressWarnings("serial")
	private static class IOExceptionWrapper extends RuntimeException {
		
		public IOExceptionWrapper(IOException e) {
			super(e);
		}
		
		public void rethrow() throws IOException {
			throw (IOException)getCause();
		}		
	}
}
