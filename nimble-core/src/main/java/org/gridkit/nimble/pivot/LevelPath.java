package org.gridkit.nimble.pivot;

import java.io.Serializable;

abstract class LevelPath implements Comparable<LevelPath>, Serializable {

	private static final long serialVersionUID = 20120423L;

	public static LevelPath root() {
		return new Root();
	}

	protected LevelPath parent;

	public LevelPath l(int i) {
		return new Level(this, i);
	}

	public LevelPath g(Object key) {
		return new Group(this, key);
	}
	
	public LevelPath parent() {
		return parent instanceof Root ? null : parent;
	}
	
	/**
	 * @return context level ID
	 */
	public abstract int l();

	public Object g() {
		throw new UnsupportedOperationException();
	}
	
	public boolean isLevel() {
		return getClass() == Level.class;
	}

	public boolean isGroup() {
		return getClass() == Group.class;
	}
	
	public LevelPath append(LevelPath path) {
		if (path instanceof Root) {
			return this;
		}
		else {
			LevelPath base = append(path.parent);
			if (path instanceof Level) {
				return base.l(((Level)path).levelId);
			}
			else {
				return base.g(((Group)path).key);
			}
		}
	}
	
	public boolean startsWith(LevelPath path) {
		LevelPath[] self = flatten();
		LevelPath[] other = path.flatten();
		if (other.length <= self.length) {
			for(int i = 0; i != other.length; ++i) {
				if (self[i].compare(other[i]) != 0) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public LevelPath subpath(int from) {
		return subpath(from, length() - from);
	}
	
	public LevelPath subpath(int from, int len) {
		LevelPath[] self = flatten();
		if (from >= self.length) {
			throw new IndexOutOfBoundsException("Index " + from + " is out of [0, " + self.length + ")");
		}
		if (from + len > self.length) {
			throw new IndexOutOfBoundsException("Index " + (from + len) + " is out of [0, " + self.length + "]");
		}
		LevelPath p = root();
		for(int i = from; i != from + len; ++i) {
			LevelPath n = self[i];
			if (n instanceof Level) {
				p = p.l(((Level)n).levelId);
			}
			else {
				p = p.g(((Group)n).key);
			}
		}
		return p;
	}
	
	@Override
	public int compareTo(LevelPath o) {
		LevelPath[] a = flatten();
		LevelPath[] b = o.flatten();
		int n = 0;
		while(true) {
			if (n >= a.length || n >= b.length) {
				return a.length - b.length;
			}
			else {				
				int c = a[n].compare(b[n]);
				if (c != 0) {
					return c;
				}
				else {
					n++;
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		return parent.hashCode() << 7 ^ nodeHash();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LevelPath)) {
			return false;
		}
		else {
			return compareTo((LevelPath)obj) == 0;
		}
	}

	protected abstract int compare(LevelPath matcher);
	
	protected abstract int nodeHash();
	
	protected abstract void append(StringBuilder buffer);
	
	private LevelPath[] flatten() {
		int len = length();
		LevelPath[] fp = new LevelPath[len];
		LevelPath p = this;
		int n = 1;
		while(!(p instanceof Root)) {
			fp[fp.length - n] = p;
			p = p.parent;
			++n;
		}
		return fp;
	}
	
	public int length() {
		int n = 0;
		LevelPath p = parent;
		while(p != null) {
			p = p.parent;
			++n; 
		}
		return n;
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		append(buffer);
		return buffer.toString();
	}

	protected static class Root extends LevelPath {
		
		private static final long serialVersionUID = 20120423L;

		public Root() {
			this.parent = null;
		}

		@Override
		public int l() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return Integer.MAX_VALUE / 3;
		}
		
		@Override
		protected int nodeHash() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected int compare(LevelPath that) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void append(StringBuilder buffer) {
			// empty
		}

		@Override
		public String toString() {
			return "<root>";
		}
	}
	
	static class Level extends LevelPath {

		private static final long serialVersionUID = 20120423L;
		
		private int levelId;
		
		public Level(LevelPath pofPath, int i) {
			this.parent = pofPath;
			levelId = i;
		}

		@Override
		public int l() {
			return levelId;
		}

		@Override
		protected int nodeHash() {
			return levelId;
		}

		@Override
		protected int compare(LevelPath that) {
			if (that instanceof Group) {
				return -1;
			}
			else {
				return levelId - ((Level)that).levelId;
			}
		}

		@Override
		protected void append(StringBuilder buffer) {
			parent.append(buffer);
			buffer.append('.').append(levelId);
		}
	}
	
	static class Group extends LevelPath {

		private static final long serialVersionUID = 20120423L;
		
		private Object key;

		public Group(LevelPath pofPath, Object key) {
			this.parent = pofPath;
			this.key = key;
		}

		@Override
		public int l() {
			return parent.l();
		}
		
		@Override
		public Object g() {
			return key;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected int compare(LevelPath that) {
			if (that instanceof Level) {
				return 1;
			}
			else {
				if (key instanceof Comparable) {
					return ((Comparable) key).compareTo(((Group)that).key);
				}
				else {
					throw new UnsupportedOperationException("Key is not comparable");
				}
			}
		}

		@Override
		protected int nodeHash() {
			return key.hashCode();
		}
		
		@Override
		protected void append(StringBuilder buffer) {
			parent.append(buffer);
			buffer.append('[').append(key).append(']');
		}
	}	
}
