package huffman;

import java.util.Iterator;

public class TableCounter extends Table<Long> {
    public TableCounter(int range) {
        super(Long.class, range);

        for (int i = 0; i < range; ++i) {
            table[i] = 0L;
        }
    }

    public long add(int key, int value) {
        return (table[key] += value);
    }

    public long count(int key) {
        return add(key, 1);
    }

    public int nonzeroCount() {
        int total = 0;
        for (int i = 0; i < table.length; ++i) {
            if (table[i] != 0) {
                ++total;
            }
        }
        return total;
    }
}
