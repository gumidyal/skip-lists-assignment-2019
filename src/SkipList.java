import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * An implementation of skip lists.
 */
public class SkipList<K, V> implements SimpleMap<K, V> {

  // +-----------+---------------------------------------------------
  // | Constants |
  // +-----------+

  /**
   * The initial height of the skip list.
   */
  static final int INITIAL_HEIGHT = 16;

  // +---------------+-----------------------------------------------
  // | Static Fields |
  // +---------------+

  static Random rand = new Random();

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * Pointers to all the front elements.
   */
  ArrayList<SLNode<K, V>> front() {
    return front.next;
  }

  /**
   * The comparator used to determine the ordering in the list.
   */
  Comparator<K> comparator;

  /**
   * The number of values in the list.
   */
  int size;

  /**
   * The current height of the skiplist.
   */
  int height;

  /**
   * The probability used to determine the height of nodes.
   */
  double prob = 0.5;

  public int getCounter;
  public int setCounter;
  public int removeCounter;

  SLNode<K, V> front;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new skip list that orders values using the specified comparator.
   */
  public SkipList(Comparator<K> comparator) {
    this.front = new SLNode<K, V>(null, null, INITIAL_HEIGHT);
    for (int i = 0; i < INITIAL_HEIGHT; i++) {
      this.front.next.set(i, null);
    } // for
    this.comparator = comparator;
    this.size = 0;
    this.height = INITIAL_HEIGHT;
    getCounter = 0;
    setCounter = 0;
    removeCounter = 0;
  } // SkipList(Comparator<K>)

  /**
   * Create a new skip list that orders values using a not-very-clever default comparator.
   */
  public SkipList() {
    this((k1, k2) -> k1.toString().compareTo(k2.toString()));
  } // SkipList()


  // +-------------------+-------------------------------------------
  // | SimpleMap methods |
  // +-------------------+

  @Override
  public V set(K key, V value) {
    int j = height - 1;

    ArrayList<SLNode<K, V>> node = search(key);
    SLNode<K, V> current = this.front;
    ArrayList<SLNode<K, V>> update = new ArrayList<SLNode<K, V>>(this.size());
    if (node == null) {
      while (j >= 0) {
        int comp = comparator.compare(current.next.get(j).key, key);
        if (comp < 0) {
          current = current.next.get(j);
          update.set(j, current);
        } else if (comp == 0) {
          current.next.get(j).value = value;
        } else {
          int newLevel = rand.nextInt();
          if (newLevel > j) {
            for (int i = j + 1; i >= newLevel; i--) {
              update.set(j, this.front);
            }
            j = newLevel;
          }
          current = new SLNode<K, V>(key, value, newLevel);
          for (int i = 1; i <= newLevel; i++) {
            current.next = update.get(i).next;
            update.get(i).next.set(i, current);
          }
        }
      } // while
      return current.value;
    }
    return node.get(j).value;
  } // set function



  @Override
  public V get(K key) {
    ArrayList<SLNode<K, V>> node = search(key);
    if (node == null) {
      throw new NullPointerException("null key");
    } else {
      return node.get(0).value;
    }
  } // get(K,V)

  @Override
  public int size() {
    return this.size;
  } // size()

  @Override
  public boolean containsKey(K key) {
    ArrayList<SLNode<K, V>> node = search(key);
    if (node == null) {
      return false;
    } else {
      return true;
    }
  } // containsKey(K)

  @SuppressWarnings("unchecked")
  @Override
  public V remove(K key) {
    this.removeCounter = 0;
    if (key == null) {
      throw new NullPointerException("null key");
    }
    if (front.next(0) == null) {
      return null;
    }
    ArrayList<SLNode<K, V>> update = (ArrayList<SLNode<K, V>>) this.front.next.clone();
    SLNode<K, V> temp = this.front;

    for (int level = this.height - 1; level >= 0; level--) {
      while (temp != null && temp.next(level) != null && precede(temp.next(level).key, key)) {
        temp = temp.next(level);
        this.removeCounter++;
      }
      update.set(level, temp);
      this.removeCounter++;
    }

    if (temp.next(0) == null || precede(key, temp.next(0).key)) {
      return null;
    } else {
      SLNode<K, V> removeNode = temp.next(0);
      this.size--;
      int removeNodeHeight = temp.next(0).getHeight();
      for (int i = 0; i < removeNodeHeight; i++) {
        if (update.get(i) == removeNode) {
          this.front.setNext(i, update.get(i).next(i).next(i));
        } else {
          update.get(i).setNext(i, update.get(i).next(i).next(i));
        }
      }

      if (removeNodeHeight >= this.height) {
        int newHeight = 0;
        while (newHeight <= INITIAL_HEIGHT && this.front.next(newHeight) != null) {
          newHeight++;
        }
        this.height = newHeight;
      }
      return removeNode.value;
    }
  } // remove(K)

  @Override
  public Iterator<K> keys() {
    return new Iterator<K>() {
      Iterator<SLNode<K, V>> nit = SkipList.this.nodes();

      @Override
      public boolean hasNext() {
        return nit.hasNext();
      } // hasNext()

      @Override
      public K next() {
        return nit.next().key;
      } // next()

      @Override
      public void remove() {
        nit.remove();
      } // remove()
    };
  } // keys()

  @Override
  public Iterator<V> values() {
    return new Iterator<V>() {
      Iterator<SLNode<K, V>> nit = SkipList.this.nodes();

      @Override
      public boolean hasNext() {
        return nit.hasNext();
      } // hasNext()

      @Override
      public V next() {
        return nit.next().value;
      } // next()

      @Override
      public void remove() {
        nit.remove();
      } // remove()
    };
  } // values()

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    Iterator<SLNode<K, V>> iter = this.nodes();
    while (iter.hasNext()) {
      SLNode<K, V> node = iter.next();
      action.accept(node.key, node.value);
    }

  } // forEach

  // +----------------------+----------------------------------------
  // | Other public methods |
  // +----------------------+

  /**
   * Dump the tree to some output location.
   */
  public void dump(PrintWriter pen) {
    // Forthcoming
  } // dump(PrintWriter)

  // +---------+-----------------------------------------------------
  // | Helpers |
  // +---------+

  /* find helper method */

  public ArrayList<SLNode<K, V>> search(K key) {
    ArrayList<SLNode<K, V>> current = this.front();
    int i = this.height - 1;
    while (i >= 0) {
      if (current.get(i).next == null) {
        i--;
      } else {
        int comp = comparator.compare(current.get(i).next.get(i).key, key);
        if (comp == 0) {
          return current.get(i).next;
        } else if (comp > 0) {
          i--;
        } else if (comp < 0) {
          current = current.get(i).next;
        } else {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Pick a random height for a new node.
   */
  int randomHeight() {
    int result = 1;
    while (rand.nextDouble() < prob) {
      result = result + 1;
    }
    return result;
  } // randomHeight()

  /**
   * Get an iterator for all of the nodes. (Useful for implementing the other iterators.)
   */
  Iterator<SLNode<K, V>> nodes() {
    return new Iterator<SLNode<K, V>>() {

      /**
       * A reference to the next node to return.
       */
      SLNode<K, V> next = SkipList.this.front().get(0);

      @Override
      public boolean hasNext() {
        return this.next != null;
      } // hasNext()

      @Override
      public SLNode<K, V> next() {
        if (this.next == null) {
          throw new IllegalStateException();
        }
        SLNode<K, V> temp = this.next;
        this.next = this.next.next.get(0);
        return temp;
      } // next();
    }; // new Iterator



  } // nodes()

  private boolean precede(K key1, K key2) {
    return this.comparator.compare(key1, key2) < 0;
  }

} // class SkipList


/**
 * Nodes in the skip list.
 */
class SLNode<K, V> {

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * The key.
   */
  K key;

  /**
   * The value.
   */
  V value;

  /**
   * Pointers to the next nodes.
   */
  ArrayList<SLNode<K, V>> next;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  /**
   * Create a new node of height n with the specified key and value.
   */
  public SLNode(K key, V value, int n) {
    this.key = key;
    this.value = value;
    this.next = new ArrayList<SLNode<K, V>>(n);
    for (int i = 0; i < n; i++) {
      this.next.add(null);
    } // for
  } // SLNode(K, V, int)

  // +---------+-----------------------------------------------------
  // | Methods |
  // +---------+

  public SLNode<K, V> next(int i) {

    return this.next.get(i);
  }

  /* taken from Sam Rebelsky's eboard */
  public void setNext(int i, SLNode<K, V> newNode) {
    this.next.set(i, newNode);
  }

  public int getHeight() {
    return this.next.size();
  }


} // SLNode<K,V>

// Citation: Henry Firestone, Kedam Habte, Giang Khuat, Sam Rebelsky
