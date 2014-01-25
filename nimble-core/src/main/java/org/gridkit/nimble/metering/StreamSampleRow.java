package org.gridkit.nimble.metering;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StreamSampleRow implements Externalizable {

	@SuppressWarnings("unchecked")
	private static final Set<Class<?>> PRIMITIVES = new HashSet<Class<?>>(Arrays.asList(Float.class, Double.class, Integer.class, Long.class, String.class, Boolean.class));
	
	public static void writeToStream(ObjectOutputStream stream, StreamSampleRow row, Set<Object> backrefs) throws IOException {
		stream.writeUnshared(row);
		for(Map.Entry<Object, Object> e: row.data.entrySet()) {
			backrefs.add(e.getKey());
			Object v = e.getValue();
			if (v != null && !PRIMITIVES.contains(v.getClass())) {
				backrefs.add(v);
			}
			if (row.sharedStrings && v instanceof String) {
				backrefs.add(v);
			}
		}		
	}

	transient Map<Object, Object> data;
	transient boolean sharedStrings = true;	
	
	@Deprecated
	public StreamSampleRow() {
	}

	public StreamSampleRow(Map<Object, Object> data) {
		this.data = data;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		int n = data.size();
		out.writeShort(n);
		for(Map.Entry<Object, Object> e: data.entrySet()) {
			out.writeObject(e.getKey());
			Object v = e.getValue();
			if (v == null) {
				out.writeByte(0);
			}
			else if (v instanceof Float) {
				out.writeByte(1);
				out.writeFloat(((Float)v).floatValue());
			}
			else if (v instanceof Double) {
				out.writeByte(2);
				out.writeDouble(((Double)v).doubleValue());
			}
			else if (v instanceof Integer) {
				out.writeByte(3);
				out.writeInt(((Integer)v).intValue());
			}
			else if (v instanceof Long) {
				out.writeByte(4);
				out.writeLong(((Long)v).longValue());
			}
			else if (v instanceof Boolean) {
				out.writeByte(5);
				out.writeBoolean(((Boolean)v).booleanValue());
			}
			else if (!sharedStrings && (v instanceof String)) {
				out.writeByte(6);
				out.writeUTF((String)v);
			}
			else {
				out.writeByte(100);
				out.writeObject(v);
			}
		}			
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int size = in.readShort();
		data = new HashMap<Object, Object>(size); 
		for(int i = 0; i != size; ++i) {
			Object key = in.readObject();
			int tag = in.readByte();
			switch(tag) {
			case 0: data.put(key, null); break;
			case 1: data.put(key, in.readFloat()); break;
			case 2: data.put(key, in.readDouble()); break;
			case 3: data.put(key, in.readInt()); break;
			case 4: data.put(key, in.readLong()); break;
			case 5: data.put(key, in.readBoolean()); break;
			case 6: data.put(key, in.readUTF()); break;
			case 100: data.put(key, in.readObject()); break;
			default:
				throw new IOException("Broken stream, unknown tag " + tag);
			}
		}
	}
}