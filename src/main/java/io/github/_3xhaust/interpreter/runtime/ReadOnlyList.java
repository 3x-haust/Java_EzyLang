package io.github._3xhaust.interpreter.runtime;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public final class ReadOnlyList extends AbstractList<Object> implements RandomAccess {
    private final List<Object> items;

    public ReadOnlyList(List<Object> items) {
        this.items = items;
    }

    @Override
    public Object get(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
