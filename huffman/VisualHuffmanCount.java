package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;

import java.util.concurrent.atomic.AtomicReference;

public class VisualHuffmanCount extends ProgressableTask {
    private final HuffmanCoding encoder = new HuffmanCoding();
    private final ObservableList<String> counts;
    private final ObservableList<String> encodings;
    private final CheckMenuItem force;
    private final String source;
    private final TaskPhase[] phases = new TaskPhase[] {
        new TaskPhase("Counting character occurrences...", 0.0, this::countCharacters),
        new TaskPhase("Updating character count tab...", 0.8, this::updateCharacterCount),
        new TaskPhase("Finished counting characters...", 1., (AtomicReference<Double> ignored) -> {
            cleanup();
        })
    };

    public VisualHuffmanCount(CheckMenuItem forced, ObservableList<String> counts, ObservableList<String> encodings, String source) {
        this.counts = counts;
        this.encodings = encodings;
        this.source = source;
        this.force = forced;
    }

    @Override
    public TaskPhase[] getPhases() {
        return phases;
    }

    @Override
    public void cleanup() {

    }

    private void countCharacters(AtomicReference<Double> progress) {
        encoder.readCounts(source, progress);
    }

    private void updateCharacterCount(AtomicReference<Double> progress) {
        Platform.runLater(() -> {
            counts.clear();

            for (var entry : encoder.getCounter()) {
                counts.add(entry.toString());
            }
        });
    }
}
