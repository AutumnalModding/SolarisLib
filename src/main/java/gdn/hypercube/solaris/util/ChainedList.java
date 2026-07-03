package gdn.hypercube.solaris.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChainedList<T> {
    private final List<T> backing;

    public ChainedList(List<T> backing) {
        this.backing = backing;
    }

    public ChainedList() {
        this.backing = new ArrayList<>();
    }

    public ChainedList<T> add(T element) {
        this.backing.add(element);
        return this;
    }

    public ChainedList<T> add(int index, T element) {
        this.backing.add(index, element);
        return this;
    }

    public ChainedList<T> remove(int index) {
        this.backing.remove(index);
        return this;
    }

    public ChainedList<T> remove(T element) {
        this.backing.remove(element);
        return this;
    }

    public ChainedList<T> set(int index, T element) {
        this.backing.set(index, element);
        return this;
    }

    public ChainedList<T> addFirst(T element) {
        this.backing.addFirst(element);
        return this;
    }

    public ChainedList<T> addLast(T element) {
        this.backing.addLast(element);
        return this;
    }

    public ChainedList<T> removeFirst() {
        this.backing.removeFirst();
        return this;
    }

    public ChainedList<T> removeLast() {
        this.backing.removeLast();
        return this;
    }

    public ChainedList<T> addAll(Collection<? extends T> target) {
        this.backing.addAll(target);
        return this;
    }

    public ChainedList<T> addAll(int index, Collection<? extends T> target) {
        this.backing.addAll(index, target);
        return this;
    }

    public ChainedList<T> removeAll(Collection<? extends T> target) {
        this.backing.removeAll(target);
        return this;
    }

    // Why would you want this?
    public ChainedList<T> clear() {
        this.backing.clear();
        return this;
    }

    public ArrayList<T> arrayify() {
        return new ArrayList<>(this.backing);
    }
}
