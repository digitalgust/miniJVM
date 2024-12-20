package org.mini.gui;

import java.util.*;
import java.util.function.Predicate;

class ChildList<T extends GObject> extends ArrayList<T> {

    public synchronized T set(int index, T element) {
        if (get(index).getLayer() == GObject.LAYER_INNER) {
            return null;
        }
        return super.set(index, element);
    }

    @Override
    public synchronized T remove(int index) {
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
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex || fromIndex < 0 || toIndex >= size()) {
            System.out.println("removeRange error");
            return;
        }
        for (int i = fromIndex; i < toIndex; i++) {
            remove(i);
        }
    }

    @Override
    public synchronized boolean remove(Object o) {
        if (o == null || !(o instanceof GObject)) return false;
        if (((GObject) o).getLayer() == GObject.LAYER_INNER) return false;
        return super.remove(o);
    }

    @Override
    public synchronized boolean removeAll(Collection c) {
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
    public synchronized boolean removeIf(Predicate filter) {
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
    public synchronized void clear() {
        for (int i = size() - 1; i >= 0; i--) {
            remove(i);
        }
    }

    public synchronized List<T> subList(int fromIndex, int toIndex) {
        ChildList<T> subList = new ChildList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            T go = get(i);
            if (((GObject) go).getLayer() == GObject.LAYER_INNER) continue;
            subList.add(get(i));
        }
        return subList;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        for (int i = 0; i < size(); i++) {
            T go = get(i);
            if (((GObject) go).getLayer() == GObject.LAYER_INNER) continue;
            if (!c.contains(go)) {
                remove(i);
                i--;
            }
        }
        return false;
    }

    @Override
    public synchronized ListIterator<T> listIterator() {
        ListIterator<T> li = new MyListItr(0);
        return li;
    }

    @Override
    public synchronized ListIterator<T> listIterator(int index) {
        ListIterator<T> li = new MyListItr(index);
        return li;
    }

    private class MyListItr extends MyItr implements ListIterator<T> {
        MyListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public T previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new IndexOutOfBoundsException();
            if (i >= size())
                throw new ConcurrentModificationException();
            cursor = i;
            lastRet = i;
            return get(i);
        }

        public void set(T e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                T t = ChildList.this.get(lastRet);
                if (t.getLayer() == GObject.LAYER_INNER) {
                    System.out.println("This element can't be set " + t);
                    return;
                }
                ChildList.this.set(lastRet, e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public void add(T e) {
            checkForComodification();

            try {
                int i = cursor;
                ChildList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public synchronized Iterator<T> iterator() {
        //System.out.println("================" + size() + "  ," + modCount);
        return new MyItr();
    }

    class MyItr implements Iterator<T> {
        int cursor = 0;
        int lastRet = -1;
        int expectedModCount = ChildList.this.modCount;

        @Override
        public boolean hasNext() {
            return this.cursor < ChildList.this.size();
        }

        @Override
        public T next() {
            //System.out.println("**************" + size() + "  ," + modCount);
            this.checkForComodification();

            try {
                T next;
                next = ChildList.this.get(this.cursor);
                this.lastRet = this.cursor++;

                return next;
            } catch (IndexOutOfBoundsException var2) {
                this.checkForComodification();
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void remove() {
            if (this.lastRet == -1) {
                throw new IllegalStateException();
            } else {
                this.checkForComodification();

                try {
                    T t = ChildList.this.get(this.lastRet);
                    if (t.getLayer() == GObject.LAYER_INNER) {
                        return;
                    }
                    ChildList.this.remove(this.lastRet);
                    if (this.lastRet < this.cursor) {
                        --this.cursor;
                    }

                    this.lastRet = -1;
                    this.expectedModCount = ChildList.this.modCount;
                } catch (IndexOutOfBoundsException var2) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        final void checkForComodification() {
            if (ChildList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
