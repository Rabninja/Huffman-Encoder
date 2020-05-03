package huffman;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

public class HuffmanCoding {
    public static final byte[] INPUT_BUFFER = new byte[65536];
    public static final short SIGNATURE = 7006;

    private TableCounter counter = new TableCounter(256);
    private HuffmanTree tree = new HuffmanTree();
    private Table<String> encoding = null;

    public class Encode extends ProgressableTask {
        private final TaskPhase[] phases = new TaskPhase[] {
            new TaskPhase("Counting character occurrences...", 0., this::count),
            new TaskPhase("Building huffman tree...", 0.45, this::createTree),
            new TaskPhase("Encoding characters...", 0.5, this::createEncodings),
            new TaskPhase("Writing to destination...", .7, this::encodeSource),
            new TaskPhase("Finished encoding file...", 1., (AtomicReference<Double> ignored) -> {
                cleanup();
            })
        };
        private final String source;
        private final String destination;

        public Encode(String source, String destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public TaskPhase[] getPhases() {
            return phases;
        }

        @Override
        public void cleanup() {

        }

        private void count(AtomicReference<Double> progress) throws IOException {
            readCounts(source, progress);
        }

        private void createTree(AtomicReference<Double> progress) throws IOException {
            buildTree();
        }

        private void createEncodings(AtomicReference<Double> progress) throws IOException {
            buildEncodings();
        }

        private void encodeSource(AtomicReference<Double> progress) throws IOException {
            writeToFile(source, destination, progress);
        }
    }

    public class Decode extends ProgressableTask {
        private final TaskPhase[] phases = new TaskPhase[] {
            new TaskPhase("Open huffman file...", 0., this::openSource),
            new TaskPhase("Verifying huffman file...", 0., this::verifySource),
            new TaskPhase("Extracting huffman tree...", 0.1, this::extractTree),
            new TaskPhase("Decoding file...", .7, this::decodeSource),
            new TaskPhase("Finished decoding file...", 1., (AtomicReference<Double> ignored) -> {
                cleanup();
            })
        };
        private InputStream input;
        private final String source;
        private final String destination;
        private long length;

        public Decode(String source, String destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public TaskPhase[] getPhases() {
            return phases;
        }

        @Override
        public void cleanup() {
            if (input != null) {
                try {
                    input.close();
                    input = null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        private void openSource(AtomicReference<Double> progress) throws IOException {
            length = new File(source).length();
            final FileInputStream fin = new FileInputStream(source);
            input = new BufferedInputStream(fin);
        }

        private void verifySource(AtomicReference<Double> progress) throws IOException {
            if (!validateHuffman(input)) {
                throw new VisualHuffmanDecoding.InvalidHuffman();
            }
        }

        private void extractTree(AtomicReference<Double> progress) throws IOException {
            extractEncodings(input);
        }

        private void decodeSource(AtomicReference<Double> progress) throws IOException {
            decodeToFile(input, destination, length, progress);
        }
    }

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

    public static class HuffmanBadHeader extends HuffmanException {
        public HuffmanBadHeader(String explanation) {
            super(explanation);
        }
    }

    public static class HuffmanIOExcept extends HuffmanException {
        public HuffmanIOExcept() {
            super("Critical error occurred when processing the source/destination file.");
        }
    }

    public HuffmanCoding() {

    }

    public Encode getEncoder(String source, String destination) {
        return new Encode(source, destination);
    }

    public Decode getDecoder(String source, String destination) {
        return new Decode(source, destination);
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

    public long getHeaderBytes() {
        return Short.BYTES + (tree.toString().length() + Byte.SIZE - 1) / Byte.SIZE + 2;
    }

    public long getTotalCompressionBytes() {
        final long header = getHeaderBytes();
        final long translated = (getCompressionSize() + Byte.SIZE - 1) / Byte.SIZE;

        return header + translated;
    }

    public Table<String> getEncodings() {
        return encoding;
    }

    public TableCounter getCounter() {
        return counter;
    }

    private void buildTree() {
        tree.initialize(counter);
    }

    private void buildEncodings() {
        encoding = tree.getEncodings();
    }

    public void readCounts(String source, AtomicReference<Double> progress) throws HuffmanIOExcept {
        final long length = new File(source).length();
        long total = 0;
        FileInputStream file = null;
        try {
            file = new FileInputStream(source);

            BufferedInputStream stream = new BufferedInputStream(file);

            counter = new TableCounter(256);
            while (stream.available() > 0) {
                int read = stream.read(INPUT_BUFFER, 0, Math.min(INPUT_BUFFER.length, stream.available()));

                for (int i = 0; i < read; ++i) {
                    counter.count(INPUT_BUFFER[i] & 0xFF);
                }
                total += read;
                progress.set((double)total / length);
            }
        } catch (IOException ioe) {
            throw new HuffmanIOExcept();
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

    private boolean validateHuffman(InputStream input) throws HuffmanIOExcept {
        try {
            input.read(INPUT_BUFFER, 0, 2);
        } catch (IOException ioe) {
            throw new HuffmanIOExcept();
        }
        return SIGNATURE == (short) (((INPUT_BUFFER[0] & 0xFF) << 8) | (INPUT_BUFFER[1] & 0xFF));
    }

    private boolean extractEncodings(InputStream input) throws HuffmanIOExcept, HuffmanBadHeader {
        final int MINIMUM_REMAINING = 1;
        final int leafs;
        try {
            leafs = input.read();

            if (leafs < 0) {
                throw new HuffmanBadHeader("Source file is not valid a Huffman encoding, it has an impossible amount of leafs.");
            }
            if (input.available() > MINIMUM_REMAINING) {
                tree.fromStream(256, leafs + 1, input);
            }
        }
        catch (IOException ioe) {
            throw new HuffmanIOExcept();
        }
        encoding = tree.getEncodings();

        return true;
    }

    private void decodeToFile(InputStream input, String pathway, long length, AtomicReference<Double> progress) throws HuffmanIOExcept {
        FileOutputStream fout = null;
        long total = getHeaderBytes();
        try {
            fout = new FileOutputStream(pathway);

            int slot = 0;
            final byte[] outBuffer = new byte[65536*Byte.SIZE];
            final int paddedBit = input.read();

            if (paddedBit < 0 || paddedBit > 8) {
                throw new HuffmanBadHeader("File is invalid, has impossible amount of padding.");
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
                total += read;
                progress.set((double)total / length);
            }
            fout.flush();
        } catch (IOException ioe) {
            throw new HuffmanIOExcept();
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

    private void writeToFile(String source, String destination, AtomicReference<Double> progress) throws HuffmanIOExcept {
        final long length = new File(source).length();
        long total = 0;
        FileInputStream fin = null;
        FileOutputStream fout = null;
        final StringBuilder builder = new StringBuilder();
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
                total += read;
                progress.set((double)total / length);
            }
            dumpBuilder(output, builder);
            output.flush();
        } catch (IOException ioe) {
            throw new HuffmanIOExcept();
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

    private byte byteFromBinary(StringBuilder builder, int start) {
        byte value = 0;
        for (int i = 0; i != Byte.SIZE; ++i) {
            value = (byte)(builder.charAt(start + i) == '1' ? value * 2 + 1 : value * 2);
        }
        return value;
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

    private void printHeader(BufferedOutputStream output, StringBuilder converter) throws IOException {
        final long size = getCompressionSize();
        final int leafs = counter.nonzeroCount();
        output.write((SIGNATURE & 0xFF00) >> 8); //Write signature.
        output.write(SIGNATURE & 0x00FF);
        output.write((leafs > 0 ? leafs-1 : 0) & 0x00FF);
        converter.append(tree.toString());
        final byte padding = size == 0 ? (byte)0 : (byte)(7 - ((size-1) % 8));
        converter.append(Integer.toBinaryString((padding & 0xFF) + 0x100).substring(1));
    }
}
