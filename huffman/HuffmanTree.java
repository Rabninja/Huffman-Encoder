package huffman;

import java.io.IOException;
import java.io.InputStream;
import java.util.PriorityQueue;

public class HuffmanTree {
    public class Node implements Comparable<Node> {
        private int character;
        private long priority;
        private Node left;
        private Node right;

        public Node(int character, long priority) {
            this(character, priority, null, null);
        }

        public Node(int character, long priority, Node left, Node right) {
            this.character = character;
            this.priority = priority;
            this.left = left;
            this.right = right;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }

        public int getCharacter() {
            return character;
        }

        public boolean isCharacter() {
            return character >= 0;
        }

        @Override
        public int compareTo(Node rhs) {
            return Long.compare(priority, rhs.priority);
        }
    }

    private Node root;
    private int length;

    public HuffmanTree() {

    }

    public void initialize(TableCounter counts) {
        final PriorityQueue<Node> queue = new PriorityQueue<>();
        length = counts.getRange();

        for (var value : counts) {
            if (value.value > 0) {
                queue.add(new Node(value.index, value.value));
            }
        }

        while (queue.size() > 1) {
            final var left = queue.remove();
            final var right = queue.remove();
            queue.add(new Node(-1, left.priority+right.priority, left, right));
        }
        if (!queue.isEmpty()) {
            root = queue.remove();
        }
    }

    public Table<String> getEncodings() {
        Table<String> encoder = new Table<>(String.class, length);
        traverse(root, "", encoder);
        return encoder;
    }

    private void traverse(Node node, String encoding, Table<String> encoder) {
        if (node == null) {
            return;
        }
        if (node.character >= 0) {
            encoder.set(node.character, encoding);
        }

        traverse(node.left, encoding+"0", encoder);
        traverse(node.right, encoding+"1", encoder);
    }

    private static class StreamData {
        public int leafs;
        public int bit;
        public int focus;

        public StreamData(int leafs, int data) {
            this.leafs = leafs;
            this.bit = 1 << 6;
            this.focus = data;
        }

        public int bitPosition() {
            int count = 0;
            int test = bit;
            while (test != 0) {
                test >>= 1;
                ++count;
            }
            return count;
        }
    }

    public void fromStream(int length, int leafs, InputStream stream) {
        root = new Node(-1, 0, null, null);
        this.length = length;
        try {
            final int focus = stream.read();
            fromStream(root, new StreamData(leafs, focus), stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Node getRoot() {
        return root;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        buildTree(root, builder);
        builder.append("0".repeat(builder.length() == 0 ? 0 : 7 - (builder.length()-1) % 8));
        return builder.toString();
    }

    private void buildTree(Node node, StringBuilder builder) {
        if (node == null) {
            return;
        }
        builder.append(node.character < 0 ? '0' : '1');
        if (node.character >= 0) {
            builder.append(Integer.toBinaryString(node.character + 0x100).substring(1));
        }
        buildTree(node.left, builder);
        buildTree(node.right, builder);
    }

    private void extractNode(Node node, StreamData data, InputStream stream, boolean left) throws IOException {
        if (data.leafs > 0) {
            if (data.bit == 0) {
                data.focus = stream.read();
                data.bit = 1 << 7;
            }
            final int type = data.focus & data.bit;
            if (type != 0) {
                final int next = stream.read();
                final int shift = data.bitPosition() - 1;
                final int character = ((data.focus & (data.bit - 1)) << (Byte.SIZE - shift)) | (next >> shift);
                data.focus = next;
                data.bit >>= 1;
                data.leafs--;
                if (left) {
                    node.left = new Node(character, 0, null, null);
                }
                else {
                    node.right = new Node(character, 0, null, null);
                }
            } else {
                data.bit >>= 1;
                if (left) {
                    node.left = new Node(-1, 0, null, null);
                    fromStream(node.left, data, stream);
                }
                else {
                    node.right = new Node(-1, 0, null, null);
                    fromStream(node.right, data, stream);
                }
            }
        }
    }

    private void fromStream(Node node, StreamData data, InputStream stream) throws IOException {
        if (data.leafs == 0) {
            return;
        }
        extractNode(node, data, stream, true);
        extractNode(node, data, stream, false);
    }
}
