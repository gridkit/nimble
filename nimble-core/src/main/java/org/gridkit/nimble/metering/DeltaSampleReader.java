package org.gridkit.nimble.metering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class DeltaSampleReader implements SampleSet, Serializable {
	
	private static final long serialVersionUID = 20121101L;
	
	private Command[] commands;  
	
	public DeltaSampleReader(Command[] commands) {
		this.commands = commands;
	}
	
	public SampleReader reader() {
		return new Reader(Arrays.asList(commands).iterator());
	}
	
	static abstract class Command implements Serializable {

		private static final long serialVersionUID = 20121101L;
		
	}
	
	static class HeaderInsertCmd extends Command {

		private static final long serialVersionUID = 20121101L;
		
		final int index;
		final Object key;
		
		public HeaderInsertCmd(int index, Object key) {
			this.index = index;
			this.key = key;
		}
	}
	
	static class HeaderRemoveCmd extends Command {

		private static final long serialVersionUID = 20121101L;
		
		final int index;
		
		public HeaderRemoveCmd(int index) {
			this.index = index;
		}		
	}
	
	static class ValueSetCmd extends Command {
		
		private static final long serialVersionUID = 20121101L;
		
		final int index;
		final Object value;
		
		public ValueSetCmd(int index, Object value) {
			this.index = index;
			this.value = value;
		}
	}
	
	static class DoneCmd extends Command {		

		private static final long serialVersionUID = 20121101L;

	}
	
	private static class Reader implements SampleReader {

		private final List<Object> header = new ArrayList<Object>();
		private final Map<Object, Object> row = new HashMap<Object, Object>();
		private final Iterator<Command> commands;
		private boolean ready = false;
		
		public Reader(Iterator<Command> commands) {
			this.commands = commands;
		}

		@Override
		public boolean isReady() {
			return ready;
		}

		@Override
		public boolean next() {
			ready = false;
			while(commands.hasNext()) {
				Command cmd = commands.next();
				if (cmd.getClass() == DoneCmd.class) {
					ready = true;
					return true;
				}
				else {
					process(cmd);
				}
			}
			return false;
		}

		private void process(Command cmd) {
			if (cmd.getClass() == HeaderInsertCmd.class) {
				HeaderInsertCmd ic = (HeaderInsertCmd) cmd;
				header.add(ic.index, ic.key);
			}
			else if (cmd.getClass() == HeaderRemoveCmd.class) {
				HeaderRemoveCmd ic = (HeaderRemoveCmd) cmd;
				Object key = header.remove(ic.index);
				row.remove(key);				
			}
			else if (cmd.getClass() == ValueSetCmd.class) {
				ValueSetCmd vc = (ValueSetCmd) cmd;
				row.put(header.get(vc.index), vc.value);
			}
			else {
				throw new IllegalArgumentException("Unknown command: " + cmd);
			}
			
		}

		@Override
		public List<Object> keySet() {
			if (!ready) {
				throw new NoSuchElementException("Not ready");
			}
			else {
				return Collections.unmodifiableList(header);
			}
		}

		@Override
		public Object get(Object key) {
			if (!ready) {
				throw new NoSuchElementException("Not ready");
			}
			else {
				return row.get(key);
			}
		}
	}
}
