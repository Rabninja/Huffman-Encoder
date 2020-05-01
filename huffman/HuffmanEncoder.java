package huffman;

import java.io.*;

public class HuffmanEncoder {
    private static final byte[] INPUT_BUFFER = new byte[65536];
    private static final short SIGNATURE = 7006;
    private TableCounter counter = new TableCounter(256);
    private HuffmanTree tree = new HuffmanTree();
    private Table<String> encoding = null;

    public static abstract class HuffmanException extends RuntimeException {
        public HuffmanException(String message) {
            super(message);
        }
    }

    public static class HuffmanBadSignature extends HuffmanException {
        public HuffmanBadSignature() {
            super("Signature doesn't match huffman.");
        }
    }

    public HuffmanEncoder(String filepath) throws IOException {
        initialize(filepath);
    }

    public HuffmanEncoder() {

    }

    public void initialize(String filepath) throws IOException {
        FileInputStream file = null;
        try {
            file = new FileInputStream(filepath);
            BufferedInputStream stream = new BufferedInputStream(file);

            counter = new TableCounter(256);
            while (stream.available() > 0) {
                int read = stream.read(INPUT_BUFFER, 0, Math.min(INPUT_BUFFER.length, stream.available()));

                for (int i = 0; i < read; ++i) {
                    counter.count(INPUT_BUFFER[i] & 0xFF);
                }
            }
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void buildTree() {
        tree.initialize(counter);
    }

    public void buildEncodings() {
        encoding = tree.getEncodings();
    }

    public long getUncompressedBytes() {
        long size = 0;
        for (var entry : counter) {
            if (entry.value > 0) {
                size += entry.value;
            }
        }
        return size;
    }

    public long getCompressionSize() {
        long size = 0;
        for (var entry : counter) {
            if (entry.value > 0) {
                size += entry.value * encoding.get(entry.index).length();
            }
        }
        return size;
    }

    public long headerBytes() {
        return Short.BYTES + (tree.toString().length() + Byte.SIZE - 1) / Byte.SIZE + 2;
    }

    public long getTotalCompressionBytes() {
        final long header = headerBytes();
        final long translated = (getCompressionSize() + Byte.SIZE - 1) / Byte.SIZE;

        return header + translated;
    }

    public boolean validateHuffman(InputStream input) throws IOException {
            input.read(INPUT_BUFFER, 0, 2);
            return SIGNATURE == (short) (((INPUT_BUFFER[0] & 0xFF) << 8) | (INPUT_BUFFER[1] & 0xFF));
    }

    public boolean extractEncodings(InputStream input) throws IOException {
        final int MINIMUM_REMAINING = 1;
        final int leafs = input.read();
        if (leafs < 0) {
            throw new IOException("File is invalid, has impossible amount of leafs.");
        }
        if (input.available() > MINIMUM_REMAINING) {
            tree.fromStream(256, leafs+1, input);
        }
        encoding = tree.getEncodings();

        return true;
    }


    public void decodeTo(InputStream input, String pathway) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(pathway);

            int slot = 0;
            final byte[] outBuffer = new byte[65536*Byte.SIZE];
            final int paddedBit = input.read();

            if (paddedBit < 0 || paddedBit > 8) {
                throw new IOException("File is invalid, has impossible amount of padding.");
            }
            final int extra = 1 << paddedBit;
            var node = tree.getRoot();
            SCANNING:
            while (input.available() > 0) {
                final int read = input.read(INPUT_BUFFER, 0, Math.min(INPUT_BUFFER.length, input.available()));
                final int available = input.available();
                for (int i = 0; i < read; ++i) {
                    final boolean last = available == 0 && i - read == -1;
                    int focus = INPUT_BUFFER[i] & 0xFF;
                    for (int bit = 1 << 7; bit != 0; bit >>= 1) {
                        if ((focus & bit) == 0) {
                            node = node.getLeft();
                        }
                        else {
                            node = node.getRight();
                        }

                        if (node.isCharacter()) {
                            outBuffer[slot++] = (byte)node.getCharacter();
                            node = tree.getRoot();
                        }
                        if (last && extra == bit) {
                            fout.write(outBuffer, 0, slot);
                            break SCANNING;
                        }
                    }
                }
                fout.write(outBuffer, 0, slot);
                slot = 0;
            }
            fout.flush();
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToFile(String source, String destination) throws IOException {
        FileInputStream fin = null;
        FileOutputStream fout = null;
        StringBuilder builder = new StringBuilder();
        try {
            fin = new FileInputStream(source);
            fout = new FileOutputStream(destination);

            BufferedInputStream input = new BufferedInputStream(fin);
            BufferedOutputStream output = new BufferedOutputStream(fout, 65536);
            printHeader(output, builder);
            dumpBuilder(output, builder);
            while (input.available() > 0) {
                int read = input.read(INPUT_BUFFER, 0, Math.min(INPUT_BUFFER.length, input.available()));
                for (int i = 0; i < read; ++i) {
                    builder.append(encoding.get(INPUT_BUFFER[i] & 0xFF));

                    if (builder.length() > 262144) {
                        for (int j = 0; j < 262144; j += Byte.SIZE) {
                            output.write(byteFromBinary(builder, j));
                        }
                        builder.delete(0, 262144);
                    }
                }
            }
            dumpBuilder(output, builder);
            output.flush();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dumpBuilder(OutputStream output, StringBuilder builder) throws IOException {
        if (builder.length() > 0) {
            builder.append("0".repeat(7 - (builder.length()-1) % 8));
            for (int j = 0; j < builder.length(); j += Byte.SIZE) {
                final byte value = byteFromBinary(builder, j);
                output.write(value);
            }
            builder.setLength(0);
        }
    }

    public Table<String> getEncodings() {
        return encoding;
    }

    public TableCounter getCounter() {
        return counter;
    }

    private byte byteFromBinary(StringBuilder builder, int start) {
        byte value = 0;
        for (int i = 0; i != Byte.SIZE; ++i) {
            value = (byte)(builder.charAt(start + i) == '1' ? value * 2 + 1 : value * 2);
        }
        return value;
    }

    private void printHeader(BufferedOutputStream output, StringBuilder converter) {
        try {
            final long size = getCompressionSize();
            final int leafs = counter.nonzeroCount();
            output.write((SIGNATURE & 0xFF00) >> 8); //Write signature.
            output.write(SIGNATURE & 0x00FF);
            output.write((leafs > 0 ? leafs-1 : 0) & 0x00FF);
            converter.append(tree.toString());
            final byte padding = size == 0 ? (byte)0 : (byte)(7 - ((size-1) % 8));
            converter.append(Integer.toBinaryString((padding & 0xFF) + 0x100).substring(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
