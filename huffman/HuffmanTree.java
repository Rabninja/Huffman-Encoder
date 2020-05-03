package huffman;

import java.io.IOException;
import java.io.InputStream;
import java.util.PriorityQueue;

public class HuffmanTree {
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

    public static class TreeNode implements Comparable<TreeNode> {
        private int character;
        private long priority;
        private TreeNode left;
        private TreeNode right;

        public TreeNode(int character, long priority) {
            this(character, priority, null, null);
        }

        public TreeNode(int character, long priority, TreeNode left, TreeNode right) {
            this.character = character;
            this.priority = priority;
            this.left = left;
            this.right = right;
        }

        public TreeNode getLeft() {
            return left;
        }

        public TreeNode getRight() {
            return right;
        }

        public int getCharacter() {
            return character;
        }

        public boolean isCharacter() {
            return character >= 0;
        }

        @Override
        public int compareTo(TreeNode rhs) {
            return Long.compare(priority, rhs.priority);
        }
    }

    private TreeNode root;
    private int length;

    public HuffmanTree() {

    }

    public void initialize(TableCounter counts) {
        final PriorityQueue<TreeNode> queue = new PriorityQueue<>();
        length = counts.getRange();

        for (var value : counts) {
            if (value.value > 0) {
                queue.add(new TreeNode(value.index, value.value));
            }
        }

        while (queue.size() > 1) {
            final var left = queue.remove();
            final var right = queue.remove();
            queue.add(new TreeNode(-1, left.priority+right.priority, left, right));
        }
        if (!queue.isEmpty()) {
            root = queue.remove();

            if (root.left == null) {
                root = new TreeNode(-1, root.priority, root, null);
            }
        }
    }

    public Table<String> getEncodings() {
        Table<String> encoder = new Table<>(String.class, length);
        traverse(root, "", encoder);
        return encoder;
    }

    public void fromStream(int length, int leafs, InputStream stream) {
        root = new TreeNode(-1, 0, null, null);
        this.length = length;
        try {
            final int focus = stream.read();
            fromStream(root, new StreamData(leafs, focus), stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TreeNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        buildTree(root, builder);
        builder.append("0".repeat(builder.length() == 0 ? 0 : 7 - (builder.length()-1) % 8));
        return builder.toString();
    }

    private void buildTree(TreeNode node, StringBuilder builder) {
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

    private void extractNode(TreeNode node, StreamData data, InputStream stream, boolean left) throws IOException {
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
                    node.left = new TreeNode(character, 0, null, null);
                }
                else {
                    node.right = new TreeNode(character, 0, null, null);
                }
            } else {
                data.bit >>= 1;
                if (left) {
                    node.left = new TreeNode(-1, 0, null, null);
                    fromStream(node.left, data, stream);
                }
                else {
                    node.right = new TreeNode(-1, 0, null, null);
                    fromStream(node.right, data, stream);
                }
            }
        }
    }

    private void fromStream(TreeNode node, StreamData data, InputStream stream) throws IOException {
        if (data.leafs == 0) {
            return;
        }
        extractNode(node, data, stream, true);
        extractNode(node, data, stream, false);
    }

    private void traverse(TreeNode node, String encoding, Table<String> encoder) {
        if (node == null) {
            return;
        }
        if (node.character >= 0) {
            encoder.set(node.character, encoding);
        }

        traverse(node.left, encoding+"0", encoder);
        traverse(node.right, encoding+"1", encoder);
    }
}
