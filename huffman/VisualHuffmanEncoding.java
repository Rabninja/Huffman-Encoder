package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;

public class VisualHuffmanEncoding extends ProgressableTask {
    private final HuffmanCoding encoder = new HuffmanCoding();
    private final ObservableList<String> counts;
    private final ObservableList<String> encodings;
    private final CheckMenuItem force;
    private final String source;
    private final String destination;
    private HuffmanCoding.Encode encode;
    private int encodeStep = 0;

    private final TaskPhase[] phases = new TaskPhase[] {
        new TaskPhase("Initializing encoder...", 0., this::initialize),
        new TaskPhase("Counting character occurrences...", 0.05, this::encodeStep),
        new TaskPhase("Updating character count tab...", 0.30, this::updateCharacterCount),
        new TaskPhase("Building huffman tree...", 0.40, this::encodeStep),
        new TaskPhase("Encoding characters...", 0.45, this::encodeStep),
        new TaskPhase("Verifying encoding size...", 0.50, this::verifyEncodingSize),
        new TaskPhase("Update character encoding tab...", 0.6, this::updateCharacterEncoding),
        new TaskPhase("Writing to destination...", .65, this::encodeStep),
        new TaskPhase("Finished encoding file...", 1., this::cleanup)
    };

    public static class PoorEncodingException extends RuntimeException {
        public PoorEncodingException(long compressed, long uncompressed) {
            super("Encoding results in a larger file(" + uncompressed + " -> " + compressed + "). Enable force encoding to force encoding operation.");
        }
    }

    public VisualHuffmanEncoding(CheckMenuItem forced, ObservableList<String> counts, ObservableList<String> encodings, String source, String destination) {
        this.counts = counts;
        this.encodings = encodings;
        this.source = source;
        this.destination = destination;
        this.force = forced;
    }

    @Override
    public TaskPhase[] getPhases() {
        return phases;
    }

    @Override
    public void cleanup() {
        encode.cleanup();
    }

    private void updateCharacterCount() {
        Platform.runLater(() -> {
            counts.clear();

            for (var entry : encoder.getCounter()) {
                counts.add(entry.toString());
            }
        });
    }

    private void updateCharacterEncoding() {
        Platform.runLater(() -> {
            encodings.clear();

            for (var encoding : encoder.getEncodings()) {
                encodings.add(encoding.toString());
            }
        });
    }

    private void initialize() {
        encode = encoder.getEncoder(source, destination);
    }

    private void encodeStep() throws Exception {
        encode.getPhases()[encodeStep++].run();
    }

    private void verifyEncodingSize() {
        final long compressedBytes = encoder.getTotalCompressionBytes();
        final long uncompressedBytes = encoder.getUncompressedBytes();
        if (!force.isSelected() && compressedBytes > uncompressedBytes) {
            throw new PoorEncodingException(compressedBytes, uncompressedBytes);
        }
    }
}
