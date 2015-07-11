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
	//comparison for any object
	final static boolean valEquals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
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

	final Entry<K,V> getLastEntry() {
	
		Entry<K,V> p = root;
		if (p != null)
			while (p.right != null)
				p = p.right;
		return p;
	}

	final Entry<K,V> getFirstEntry() {
	
		Entry<K,V> p = root;
		if (p != null)
			while (p.left != null)
				p = p.left;
		return p;
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
				fixAfterInsertion(e);
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
			public SortedMap<K, V> subMap(K arg0, K arg1) {
				// TODO Auto-generated method stub
				return null;
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

			class Entry <K,V> implements Map.Entry<K, V> {
			
				int height;
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
			class   EntrySet extends AbstractSet<Map.Entry<K,V>> {

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
