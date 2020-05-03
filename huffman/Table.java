package huffman;

import java.lang.reflect.Array;
import java.util.Iterator;

public class Table<E> implements Iterable<Table.TablePair<E>> {
    protected E[] table;

    public static class TablePair<E> {
        public final E value;
        public final int index;

        public TablePair(int index, E value) {
            this.value = value;
            this.index = index;
        }

        @Override
        public String toString() {
            final char INVALID_ASCII = (char)1;
            final char visual = index < 32 ? INVALID_ASCII : (char)index;
            return String.format("%-5c%-7d%-12s=   ", visual, index, Integer.toBinaryString(index)) + value;
        }
    }

    public class TableIterator implements Iterator<TablePair<E>> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < table.length;
        }

        @Override
        public TablePair<E> next() {
            final int active = index++;
            return new TablePair<>(active, table[active]);
        }
    }

    @SuppressWarnings("unchecked")
    public Table(Class<E> c, int range) {
        table = (E[])Array.newInstance(c, range);
    }

    public int getRange() {
        return table.length;
    }

    public E set(int index, E value) {
        var old = table[index];
        table[index] = value;
        return old;
    }

    public E get(int index) {
        return table[index];
    }

    @Override
    public Iterator<Table.TablePair<E>> iterator() {
        return new TableIterator();
    }
}
