package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;

public class VisualHuffmanDecoding extends ProgressableTask {
    private final HuffmanCoding encoder = new HuffmanCoding();
    private final ObservableList<String> counts;
    private final ObservableList<String> encodings;
    private final String source;
    private final String destination;
    private final TaskPhase[] phases = new TaskPhase[] {
        new TaskPhase("Initializing decoder...", 0., this::initialize),
        new TaskPhase("Opening source file...", 0.05, this::decodeStep),
        new TaskPhase("Verifying huffman file...", 0.1, this::decodeStep),
        new TaskPhase("Extracting huffman tree...", 0.2, this::decodeStep),
        new TaskPhase("Update character encoding tab...", 0.3, this::updateEncodingTab),
        new TaskPhase("Decoding file...", .4, this::decodeStep),
        new TaskPhase("Finished decoding file...", 1., this::cleanup)
    };
    private HuffmanCoding.Decode decode;
    private int decodeStep = 0;

    @Override
    public TaskPhase[] getPhases() {
        return phases;
    }

    @Override
    public void cleanup() {
        decode.cleanup();
    }

    public static class InvalidHuffman extends RuntimeException {
        InvalidHuffman() {
            super("Error reading requested file, file has an invalid signature that doesn't match huffman encoding.");
        }
    }

    public VisualHuffmanDecoding(ObservableList<String> counts, ObservableList<String> encodings, String source, String destination) {
        this.source = source;
        this.destination = destination;
        this.counts = counts;
        this.encodings = encodings;
    }

    private void updateEncodingTab() {
        Platform.runLater(() -> {
            encodings.clear();

            for (var encoding : encoder.getEncodings()) {
                encodings.add(encoding.toString());
            }
        });
    }

    private void initialize() {
        decode = encoder.getDecoder(source, destination);
    }

    private void decodeStep() throws Exception {
        decode.getPhases()[decodeStep++].run();
    }
}
