package org.mini.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

class ChildList<T extends GObject> extends ArrayList<T> {

    @Override
    public T remove(int index) {
        if (index >= size() || index < 0) {
            return null;
        }
        T go = get(index);
        if (go.getLayer() == GObject.LAYER_INNER) {
            return null;
        }
        return super.remove(index);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex || fromIndex < 0 || toIndex > size()) {
            return;
        }
        for (int i = fromIndex; i < toIndex; i++) {
            remove(i);
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        if (((GObject) o).getLayer() == GObject.LAYER_INNER) return false;
        return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection c) {
        boolean modified = false;
        Iterator e = this.iterator();

        while (e.hasNext()) {
            Object go = e.next();
            if (((GObject) go).getLayer() == GObject.LAYER_INNER) continue;
            if (c.contains(go)) {
                e.remove();
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public boolean removeIf(Predicate filter) {
        Predicate p = new Predicate() {
            @Override
            public boolean test(Object o) {
                if (((GObject) o).getLayer() == GObject.LAYER_INNER) return false;
                return filter.test(o);
            }
        };
        return super.removeIf(p);
    }

    @Override
    public void clear() {
        for (int i = size() - 1; i >= 0; i--) {
            remove(i);
        }
    }
}
