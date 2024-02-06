package org.mini.gui;

import java.util.*;
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
        if (fromIndex > toIndex || fromIndex < 0 || toIndex >= size()) {
            System.out.println("removeRange error");
            return;
        }
        for (int i = fromIndex; i < toIndex; i++) {
            remove(i);
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null || !(o instanceof GObject)) return false;
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

    public List<T> subList(int fromIndex, int toIndex) {
        System.out.println("this container not support subList()");
        return null;
    }

//    @Override
//    public boolean removeAll(Collection<? extends T> c) {
//        System.out.println("this container not support replaceAll()");
//        return false;
//    }

    @Override
    public boolean retainAll(Collection<?> c) {
        System.out.println("this container not support retainAll()");
        return false;
    }

    @Override
    public ListIterator<T> listIterator() {
        System.out.println("this container not support listIterator()");
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        System.out.println("================" + size() + "  ," + modCount);
        return new Iterator<T>() {
            int cursor = 0;
            int lastRet = -1;
            int expectedModCount = ChildList.this.modCount;

            @Override
            public boolean hasNext() {
                return this.cursor != ChildList.this.size();
            }

            @Override
            public T next() {
                System.out.println("**************" + size() + "  ," + modCount);
                this.checkForComodification();

                try {
                    T next;
                    do {
                        next = ChildList.this.get(this.cursor);
                        this.lastRet = this.cursor++;
                    } while (next.getLayer() == GObject.LAYER_INNER);

                    return next;
                } catch (IndexOutOfBoundsException var2) {
                    this.checkForComodification();
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                if (this.lastRet == -1) {
                    throw new IllegalStateException();
                } else {
                    this.checkForComodification();

                    try {
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
        };
    }
}
