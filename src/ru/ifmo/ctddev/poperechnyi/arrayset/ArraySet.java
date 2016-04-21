package ru.ifmo.ctddev.poperechnyi.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> array;
    private final boolean naturalOrder;
    private final Comparator<T> comparator;
    private final boolean reversed;

    public ArraySet() {
        this(new ArrayList<>(), (x, y) -> 0, true);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(Collection<? extends T> collection) {
        this(collection, (x, y) -> ((Comparable<? super T>) x).compareTo(y), true);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<T> comparator) {
        this(collection, comparator, false);
    }

    private ArraySet(Collection<? extends T> collection, Comparator<T> comparator, boolean naturalOrder) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        ArrayList<T> arr = new ArrayList<>();
        arr.addAll(treeSet);
        reversed = false;
        this.comparator = comparator;
        this.array = new ArrayList<>(arr);
        this.naturalOrder = naturalOrder;
    }

    public ArraySet(List<T> array, Comparator<T> comparator, boolean naturalOrder, boolean reversed) {
        this.array = array;
        this.comparator = comparator;
        this.naturalOrder = naturalOrder;
        this.reversed = reversed;
    }

    private T get(int i) {
        return reversed ? array.get(array.size() - 1 - i) : array.get(i);
    }

    private Comparator<T> getComparator() {
        return comparator;
    }

    @Override
    public T lower(T t) {
        int i = search(t, getComparator());
        if (i <= 0) return null;
        return get(i - 1);
    }

    @Override
    public T floor(T t) {
        int i = search(t, getComparator());
        if (i < 0) {
            return null;
        }
        if (i == array.size() || getComparator().compare(get(i), t) != 0) {
            return i > 0 ? get(i - 1):null;
        }
        return get(i);
    }

    @Override
    public T ceiling(T t) {
        int i = search(t, getComparator());
        if (i >= array.size()) {
            return null;
        }
        return get(i);
    }

    @Override
    public T higher(T t) {
        int i = search(t, getComparator());
        if (i >= array.size()) {
            return null;
        }
        if (getComparator().compare(t, get(i)) == 0) {
            return i<array.size() - 1?  get(i + 1):null;
        } else {
            return get(i);
        }
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst(): ArraySet is immutable");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast(): ArraySet is immutable");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return super.addAll(c);
    }


    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(array, comparator, naturalOrder, !reversed);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public boolean add(T t) {
        return super.add(t);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return headSet(toElement, toInclusive).tailSet(fromElement, fromInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int i = search(toElement, getComparator());
        boolean check = (!inclusive || !(i < array.size() && getComparator().compare(get(i), toElement) == 0));
        return new ArraySet<>(array.subList(0,check?i:i+1), comparator,  naturalOrder, reversed);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int i = search(fromElement, comparator);
        boolean check = !inclusive && i < array.size() && getComparator().compare(get(i), fromElement) == 0;
        return new ArraySet<>(array.subList(check?i+1:i,array.size()), comparator, naturalOrder, reversed);
    }

    @Override
    public Comparator<? super T> comparator() {
        return naturalOrder ? null : comparator;
    }


    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) throw new NoSuchElementException();
        return get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) throw new NoSuchElementException();
        return get(array.size() - 1);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        if (o == null) throw new NullPointerException();
        T item = (T) o;
        int i = search(item, getComparator());
        return i >= 0 && i < array.size() && comparator.compare(get(i), item) == 0;
    }

    private int search(T elem, Comparator<T> comp) {
        int res = Collections.binarySearch(array,elem, comp);
        return res >= 0 ? (reversed ? array.size() - 1 - res : res) : (reversed ? -res - 2 : -res - 1);
    }
}