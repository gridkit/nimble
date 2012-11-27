package org.gridkit.nimble.pivot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;

public class CsvSampleReader implements SampleReader {

	private BufferedReader reader;
	
	private List<Object> header;
	private Map<Object, Object> row;
	
	public CsvSampleReader(Reader reader) throws IOException {
		this.reader = toBuffered(reader);
		readHeader();
	}
	
	private static BufferedReader toBuffered(Reader reader) {
		if (reader instanceof BufferedReader) {
			return (BufferedReader)reader;
		}
		else {
			return new BufferedReader(reader);
		}
	}
	
	private void readHeader() throws IOException {
		String[] line = readLine();
		if (line == null) {
			throw new IOException("File is empty");
		}
		if (line[0].equals("sep=")) {
			readHeader();
			return;
		}
		header = new ArrayList<Object>(line.length);
		for(String column: line) {
			column = column.trim();
			try {
				header.add(Measure.valueOf(column));
			}
			catch(Exception e) {
				header.add(column);
			}
		}
	}

	
	@Override
	public boolean isReady() {
		return row != null;
	}

	@Override
	public boolean next() {
		try {
			row = createRow(readLine());
		} catch (IOException e) {
			return false;
		}
		return row != null;
	}

	@Override
	public List<Object> keySet() {
		return header;
	}

	@Override
	public Object get(Object key) {
		return row.get(key);
	}

	private Map<Object, Object> createRow(String[] line) {
		if (line == null) {
			return null;
		}
		else {
			Map<Object, Object> map = new HashMap<Object, Object>();
			for(int i = 0; i != line.length; ++i) {
				Object h = header.get(i);
				Object v = parse(line[i]);
				map.put(h, v);
			}
			return map;
		}
	}
	
	private Object parse(String string) {
		String v = string.trim();
		if (v.length() == 0) {
			return null;
		}
		else {
			try {
				return Integer.parseInt(v);
			}
			catch(NumberFormatException e) {
				// ignore;
			}
			try {
				return Long.parseLong(v);
			}
			catch(NumberFormatException e) {
				// ignore;
			}
			try {
				return Double.parseDouble(v);
			}
			catch(NumberFormatException e) {
				// ignore;
			}
			return v;
		}
	}

	private String[] readLine() throws IOException {
		String line = reader.readLine();
		if (line == null) {
			return null;
		}
		else {
			return line.split(";");
		}
	}	
}
