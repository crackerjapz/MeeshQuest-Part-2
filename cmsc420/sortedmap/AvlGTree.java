package cmsc420.sortedmap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/*
 * This code is an implementation hodge podge of the TreeMap source from java located at 
 * 
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/
 * util/TreeMap.java#TreeMap.comparator%28%29
 * 
 * and the AVLTree implementation of Mark Allen Weiss at
 * 
 * http://users.cis.fiu.edu/~weiss/dsaajava/code/DataStructures/AvlTree.java
 * 
 * Some minor flourishes were added by myself, but the real credit goes to the sources.
 * 
 * I'm sure I implemented some useless functions here, just trying to 
 * make sure I have a complete implementation. If it has it in TreeMap, and it doesn't look 
 * like a Red/Black implementation, I'm throwing it in there. 
 * 
 * There are also heavy flourishes of the implementation of the AVL Tree in Rosettacode.org
 * I have modified it slightly, but the rebalancing is pulled directly from that. Any mistakes 
 * in implementation are my own. 
 */
public class AvlGTree<K, V> 
extends AbstractMap<K, V>
implements SortedMap<K, V> {

	private int modCount;
	private int g;
	private transient EntrySet entrySet = null;

	private final Comparator<? super K> comparator;

	private transient Entry <K,V> root = null;

	private transient int size = 0;

	private int height(Entry<K,V> n) {
	    if (n == null)
	        return -1;
	    return 1 + Math.max(height(n.left), height(n.right));
	}

	//initializes new AVL Tree
	public AvlGTree() {

		comparator = null;
		root = null;
		g = 1;

	}

	//initializes new AVL Tree with custom comparison
	public AvlGTree(Comparator<? super K> comp) {

		this.comparator = comp;
		root = null;
		g = 1;

	}

	public void setG(int newG){
		g = newG;
	}

	//results the root and the size count.
	@Override
	public void clear() {

		root = null;
		size = 0;

	}

	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
			if (valEquals(value, e.value))
				return true;
		return false;
	}

	@Override
	public V get(Object key) {
		Entry<K,V> p = getEntry(key);
		return (p==null ? null : p.value);
	}
	
	//checks the size, returns true if size = 0
	@Override
	public boolean isEmpty() {
		if (size == 0) return true;
		return false;
	}
	
	private void rebalance(Entry<K,V> n) {
	        setBalance(n);
	 
	        if (n.balance == -2) {
	            if (height(n.left.left) >= height(n.left.right))
	                n = rotateRight(n);
	            else
	                n = rotateLeftThenRight(n);
	 
	        } else if (n.balance == 2) {
	            if (height(n.right.right) >= height(n.right.left))
	                n = rotateLeft(n);
	            else
	                n = rotateRightThenLeft(n);
	        }
	 
	        if (n.parent != null) {
	            rebalance(n.parent);
	        } else {
	            root = n;
	        }
	    }
	 
	    private Entry<K,V> rotateLeft(Entry<K,V> a) {
	 
	        Entry<K,V> b = a.right;
	        b.parent = a.parent;
	 
	        a.right = b.left;
	 
	        if (a.right != null)
	            a.right.parent = a;
	 
	        b.left = a;
	        a.parent = b;
	 
	        if (b.parent != null) {
	            if (b.parent.right == a) {
	                b.parent.right = b;
	            } else {
	                b.parent.left = b;
	            }
	        }
	 
	        setBalance(a, b);
	 
	        return b;
	    }
	 
	    private Entry<K,V> rotateRight(Entry<K,V> a) {
	 
	    	Entry<K,V> b = a.left;
	        b.parent = a.parent;
	 
	        a.left = b.right;
	 
	        if (a.left != null)
	            a.left.parent = a;
	 
	        b.right = a;
	        a.parent = b;
	 
	        if (b.parent != null) {
	            if (b.parent.right == a) {
	                b.parent.right = b;
	            } else {
	                b.parent.left = b;
	            }
	        }
	 
	        setBalance(a, b);
	 
	        return b;
	    }
	 
	    private Entry<K,V> rotateLeftThenRight(Entry<K,V> n) {
	        n.left = rotateLeft(n.left);
	        return rotateRight(n);
	    }
	 
	    private Entry<K,V> rotateRightThenLeft(Entry<K,V> n) {
	        n.right = rotateRight(n.right);
	        return rotateLeft(n);
	    }
	 
	private void setBalance(Entry<K,V>... nodes) {
	        for (Entry<K,V> n : nodes)
	            n.balance = height(n.right) - height(n.left);
	    }
	@Override
	public V put(K key, V value) {
		// TODO Auto-generated method stub
		Entry<K,V> t = root;
		if (t == null) {
			// TBD:
			// 5045147: (coll) Adding null to an empty TreeSet should
			// throw NullPointerException
			//
			// compare(key, key); // type check
			root = new Entry<K,V>(key, value, null);
			size = 1;
			modCount++;
			return null;
		}
		int cmp;
		Entry<K,V> parent;
		// split comparator and comparable paths
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			do {
				parent = t;
				cmp = cpr.compare(key, t.key);
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else
					return t.setValue(value);
			} while (t != null);
		}
		else {
			if (key == null)
				throw new NullPointerException();
			Comparable<? super K> k = (Comparable<? super K>) key;
			do {
				parent = t;
				cmp = k.compareTo(t.key);
				if (cmp < 0)
					t = t.left;
				else if (cmp > 0)
					t = t.right;
				else
					return t.setValue(value);
			} while (t != null);
		}
		Entry<K,V> e = new Entry<K,V>(key, value, parent);
		if (cmp < 0)
			parent.left = e;
		else
			parent.right = e;
		//fixAfterInsertion(e);
		rebalance(parent);
		size++;
		modCount++;
		return null;
	
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		super.putAll(map);
	
	}

	//not implemented in part 2
	@Override
	public V remove(Object arg0) {
	
		return null;
	}

	// returns the size of the Tree
	@Override
	public int size() {
	
		return size;
	}

	//returns the comparator this Map uses
	@Override
	public Comparator<? super K> comparator() {
	
		return comparator;
	}

	final int compare(Object k1, Object k2) {
		return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
				: comparator.compare((K)k1, (K)k2);
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		EntrySet es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
		//return null;
	}

	@Override
	public K firstKey() {
		return getFirstEntry().key;
	}

	//not implemented in part 2
	@Override
	public SortedMap<K, V> headMap(K arg0) {
	
		return null;
	}

	//not implemented in part 2
	@Override
	public Set<K> keySet() {
	
		return null;
	}

	@Override
	public K lastKey() {
		return getLastEntry().key;
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
	
		return new SubMap<K,V>(fromKey, toKey, this);
		
	}

	//not implemented in Part 2
	@Override
	public SortedMap<K, V> tailMap(K arg0) {
	
		return null;
	}

	//not implemented in Part 2
	@Override
	public Collection<V> values() {
	
		return null;
	}

	//comparison for any object
	final static boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	Entry<K,V> successor(Entry<K,V> t) {
		if (t == null)
			return null;
		else if (t.right != null) {
			Entry<K,V> p = t.right;
			while (p.left != null)
				p = p.left;
			return p;
		} else {
			Entry<K,V> p = t.parent;
			Entry<K,V> ch = t;
			while (p != null && ch == p.right) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}
	Entry<K,V>  predecessor(Entry<K,V> t) {
		if (t == null)
			return null;
		else if (t.left != null) {
			Entry<K,V> p = t.left;
			while (p.right != null)
				p = p.right;
			return p;
		} else {
			Entry<K,V> p = t.parent;
			Entry<K,V> ch = t;
			while (p != null && ch == p.left) {
				ch = p;
				p = p.parent;
			}
			return p;
		}
	}

	final Entry<K,V> getEntryUsingComparator(Object key) {
		K k = (K) key;
		Comparator<? super K> cpr = comparator;
		if (cpr != null) {
			Entry<K,V> p = root;
			while (p != null) {
				int cmp = cpr.compare(k, p.key);
				if (cmp < 0)
					p = p.left;
				else if (cmp > 0)
					p = p.right;
				else
					return p;
			}
		}
		return null;
	}

	final Entry<K,V> getEntry(Object key) {
		if (comparator != null)
			return getEntryUsingComparator(key);
		if (key == null)
			throw new NullPointerException();
		Comparable<? super K> k = (Comparable<? super K>) key;
		Entry<K,V> p = root;
		while (p != null) {
			int cmp = k.compareTo(p.key);
			if (cmp < 0)
				p = p.left;
			else if (cmp > 0)
				p = p.right;
			else
				return p;
		}
		return null;
	}

	final Entry<K,V> getFirstEntry() {
	
		Entry<K,V> p = root;
		if (p != null)
			while (p.left != null)
				p = p.left;
		return p;
	}

	final Entry<K,V> getLastEntry() {

		Entry<K,V> p = root;
		if (p != null)
			while (p.right != null)
				p = p.right;
		return p;
	}

	class SubMap<K,V> implements SortedMap<K,V>{

		final K lo, hi;
		AvlGTree<K,V> m;

		public SubMap(K fromKey, K toKey, AvlGTree base) {
			lo = fromKey;
			hi = toKey;
			m = base;
		}

		final boolean tooLow(Object key) {
			int c = m.compare(key, lo);
			return c <= 0;
		}

		final boolean  tooHigh(Object key) {

			int c = m.compare(key, hi);
			return c >= 0;
		}

		final boolean  inRange(Object key) {
			return !tooLow(key) && !tooHigh(key);
		}

		@Override
		public void clear() {
			m.clear();
		}

		@Override
		public boolean containsKey(Object arg0) {
			return inRange(arg0) && m.containsKey(arg0);
			//return false;
		}

		@Override
		public boolean containsValue(Object arg0) {
			return inRange(arg0) && m.containsValue(arg0);
			//return false;
		}

		@Override
		public V get(Object key) {
			return !inRange(key)? null :  m.get(key);
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public V put(K key, V value) {
			if (!inRange(key))
				throw new IllegalArgumentException("key out of range");
			return m.put(key, value);
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> input) {
			for (Map.Entry<? extends K, ? extends V> item : input.entrySet()){
				if (inRange(item.getKey())){
					m.put(item.getKey(), item.getValue());
				}
			}
		}

		@Override
		public V remove(Object arg0) {
			// Not until Part 3
			return null;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Comparator<? super K> comparator() {
			
			return m.comparator;
		}

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public K firstKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> headMap(K arg0) {
			// not in Part 2
			return null;
		}

		@Override
		public Set<K> keySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public K lastKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SortedMap<K, V> subMap(K arg0, K arg1) {
			
			return new SubMap(arg0, arg1, m);
		}

		@Override
		public SortedMap<K, V> tailMap(K arg0) {
			// Not in Part 2
			return null;
		}

		@Override
		public Collection<V> values() {
			// TODO Auto-generated method stub
			return null;
		}

	}
	class Entry <K,V> implements Map.Entry<K, V> {

		int height;
		int balance;
		Entry<K,V> left = null;
		Entry<K,V> right = null;
		Entry<K,V> parent;
		K key;
		V value;



		Entry(K key, V value){
			this.key = key;
			this.value = value;
			height = 0;
		}

		public Entry(K key2, V value2, Entry<K, V> parent2) {
			this(key2, value2);
			parent = parent2;
		}

		@Override
		public K getKey() {

			return key;
		}

		@Override
		public V getValue() {

			return value;
		}

		@Override
		public V setValue(V val) {

			V oldValue = this.value;
			this.value = val;
			return oldValue;
		}

		//comparison
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?,?> e = (Map.Entry<?, ?>) o;

			return valEquals(key,e.getKey()) && valEquals(value, e.getValue());
		}

		//returns hashcode for each entry at the entry level
		public int hashCode() {

			int keyHash = (key==null ? 0 : key.hashCode());
			int valueHash = (value==null ? 0 : value.hashCode());
			return keyHash ^ valueHash;
		}

		//toString for each entry
		public String toString(){
			return key + "=" + value;
		}

	}
	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		Entry<K,V> next;
		Entry<K,V> lastReturned;
		int expectedModCount;
		PrivateEntryIterator(Entry<K,V> first) {
			expectedModCount = modCount;
			lastReturned = null;
			next = first;
		}

		public final boolean hasNext() {
			return next != null;
		}
		final Entry<K,V> nextEntry() {
			Entry<K,V> e = next;
			if (e == null)
				throw new NoSuchElementException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			next = successor(e);
			lastReturned = e;
			return e;
		}   final Entry<K,V> prevEntry() {
			Entry<K,V> e = next;
			if (e == null)
				throw new NoSuchElementException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			next = predecessor(e);
			lastReturned = e;
			return e;
		}
		public void  remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			// deleted entries are replaced by their successors
			if (lastReturned.left != null && lastReturned.right != null)
				next = lastReturned;
			//deleteEntry(lastReturned);
			expectedModCount = modCount;
			lastReturned = null;
		}
	}
	final class  EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
		EntryIterator(Entry<K,V> first) {
			super(first);
		}
		public Map.Entry<K,V>  next() {
			return nextEntry();
		}
	}
	class EntrySet extends AbstractSet<Map.Entry<K,V>> {

		public Iterator<Map.Entry<K,V>>  iterator() {

			return new EntryIterator(getFirstEntry());

		}

		public boolean  contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
			V value = entry.getValue();
			Entry<K,V> p = getEntry(entry.getKey());
			return p != null && valEquals(p.getValue(), value);
		}

		public boolean  remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
			V value = entry.getValue();
			Entry<K,V> p = getEntry(entry.getKey());
			if (p != null && valEquals(p.getValue(), value)) {
				//deleteEntry(p);
				return true;
			}
			return false;
		}
		public int  size() {
			return AvlGTree.this.size();

		}
		public void clear() {
			AvlGTree.this.clear();


		}

	}
}
