package org.gridkit.nimble.metering;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nimble.metering.DeltaSampleReader.*;

public class DeltaSampleWriter {

	private final List<Command> commands = new ArrayList<DeltaSampleReader.Command>();
	private final List<Object> lastHeader = new ArrayList<Object>();
	private final List<Object> lastRow = new ArrayList<Object>();

	public DeltaSampleWriter() {		
	}
	
	public SampleSet createSampleSet() {
		return new DeltaSampleReader(commands.toArray(new Command[0]));
	}
	
	public void clear() {
		commands.clear();
		lastHeader.clear();
		lastRow.clear();
	}
	
	public void addSamples(SampleReader reader) {
		if (reader.isReady() || reader.next()) {
			while(true) {
				readerSample(reader);
				if (!reader.next()) {
					break;
				}
			}
		}
	}

	private void readerSample(SampleReader reader) {
		List<Object> keySet = reader.keySet();
		int n = Math.max(lastHeader.size(), keySet.size());
		for(int i = 0; i != n; ++i) {
			Object ok = getFromList(lastHeader, i);
			Object nk = getFromList(keySet, i);
			
			if (!same(ok, nk)) {
				if (ok != null) {
					remove(i);
				}
				if (nk != null) {
					insert(i, nk);
					set(i, reader.get(nk));
				}
			}
			else {
				Object ov = lastRow.get(i);
				Object nv = reader.get(nk);
				if (!same(ov, nv)) {
					set(i, nv);
				}
			}
		}		
		done();
	}

	private void insert(int i, Object key) {
		commands.add(new HeaderInsertCmd(i, key));		
	}

	private void set(int i, Object value) {
		commands.add(new ValueSetCmd(i, value));		
	}
	
	private void remove(int i) {
		commands.add(new HeaderRemoveCmd(i));		
	}

	private void done() {
		commands.add(new DoneCmd());		
	}

	private boolean same(Object ok, Object nk) {
		if (ok == null) {
			return nk == null ? true :  false;
		}
		else {
			return ok.equals(nk);
		}
	}

	private Object getFromList(List<Object> list, int n) {		
		return list.size() > n ? list.get(n) : null;
	}	
}
