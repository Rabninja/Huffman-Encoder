package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.concurrent.atomic.AtomicReference;

public class VisualHuffmanDecoding extends ProgressableTask {
    private final HuffmanCoding encoder = new HuffmanCoding();
    private final ObservableList<String> counts;
    private final ObservableList<String> encodings;
    private final String source;
    private final String destination;
    private final TaskPhase[] phases = new TaskPhase[] {
        new TaskPhase("Initializing decoder...", 0., this::initialize),
        new TaskPhase("Opening source file...", 0.05, this::decodeStep),
        new TaskPhase("Verifying huffman file...", 0.07, this::decodeStep),
        new TaskPhase("Extracting huffman tree...", 0.08, this::decodeStep),
        new TaskPhase("Update character encoding tab...", 0.18, this::updateEncodingTab),
        new TaskPhase("Decoding file...", .2, this::decodeStep),
        new TaskPhase("Finished decoding file...", 1., (AtomicReference<Double> ignored) -> {
            cleanup();
        })
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

    private void updateEncodingTab(AtomicReference<Double> progress) {
        Platform.runLater(() -> {
            encodings.clear();

            for (var encoding : encoder.getEncodings()) {
                encodings.add(encoding.toString());
            }
        });
    }

    private void initialize(AtomicReference<Double> progress) {
        decode = encoder.getDecoder(source, destination);
    }

    private void decodeStep(AtomicReference<Double> progress) throws Exception {
        decode.getPhases()[decodeStep++].run(progress);
    }
}
