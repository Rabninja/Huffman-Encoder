package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;

public class VisualHuffmanCount extends ProgressableTask {
    private final HuffmanCoding encoder = new HuffmanCoding();
    private final ObservableList<String> counts;
    private final ObservableList<String> encodings;
    private final CheckMenuItem force;
    private final String source;
    private final TaskPhase[] phases = new TaskPhase[] {
        new TaskPhase("Counting character occurrences...", 0.25, this::countCharacters),
        new TaskPhase("Updating character count tab...", 0.50, this::updateCharacterCount),
        new TaskPhase("Finished counting characters...", 1., (RunnableTask) this::cleanup)
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

    private void countCharacters() {
        encoder.readCounts(source);
    }

    private void updateCharacterCount() {
        Platform.runLater(() -> {
            counts.clear();

            for (var entry : encoder.getCounter()) {
                counts.add(entry.toString());
            }
        });
    }
}
