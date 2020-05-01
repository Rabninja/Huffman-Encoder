package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.File;

public class EncodeFile extends ProgressableTask {
    private HuffmanEncoder encoder = new HuffmanEncoder();
    private ObservableList<String> counts;
    private ObservableList<String> encodings;
    private String out;
    private CheckMenuItem force;
    private boolean simpleCount;

    public static class ExpandedSize extends RuntimeException {
        ExpandedSize(long compressed, long uncompressed) {
            super("Encoding results in a larger file(" + uncompressed + " -> " + compressed + "). Enable force encoding to force encoding operation.");
        }
    }

    public EncodeFile(boolean simpleCount, CheckMenuItem forced, String out, ObservableList<String> counts, ObservableList<String> encodings, ProgressBar progress, Label status, File file) {
        super(progress, status, file);
        this.counts = counts;
        this.encodings = encodings;
        this.out = out;
        this.force = forced;
        this.simpleCount = simpleCount;
    }

    private void update(final String string, final Double val) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                status.setText(string);
                progress.setProgress(val);
            }
        });
    }

    @Override
    public void run() {
        final TaskPiece[] stages = new TaskPiece[] {
            new TaskPiece("Counting character occurrences...", 0., (RunnableTask) () -> {
                encoder.initialize(file.getPath());
            }),
            new TaskPiece("Updating character count tab...", 0.35, (RunnableTask) () -> {
                Platform.runLater(() -> {
                    counts.clear();

                    for (var entry : encoder.getCounter()) {
                        counts.add(entry.toString());
                    }
                });
            }),
            new TaskPiece("Building huffman tree...", 0.45, (RunnableTask) () -> {
                encoder.buildTree();
            }),
            new TaskPiece("Encoding characters...", 0.5, (RunnableTask) () -> {
                encoder.buildEncodings();

                final long compressedBytes = encoder.getTotalCompressionBytes();
                final long uncompressedBytes = encoder.getUncompressedBytes();
                if (!force.isSelected() && compressedBytes > uncompressedBytes) {
                    throw new ExpandedSize(compressedBytes, uncompressedBytes);
                }
            }),
            new TaskPiece("Update character encoding tab...", 0.6, (RunnableTask) () -> {
                Platform.runLater(() -> {
                    encodings.clear();

                    for (var encoding : encoder.getEncodings()) {
                        encodings.add(encoding.toString());
                    }
                });
            }),
            new TaskPiece("Writing to destination...", .7, (RunnableTask) () -> {
                encoder.writeToFile(file.getPath(), out);
            }),
            new TaskPiece("Finished encoding file...", 1., (RunnableTask) () -> {

            })
        };

        try {
            for (TaskPiece stage : stages) {
                update(stage.message, stage.progress);
                stage.task.run();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (simpleCount && stages[1] == stage) {
                    update("Finished counting...", 1.);
                    break;
                }
            }
        } catch (ExpandedSize expanded) {
            Platform.runLater(() -> {
                status.setText("Stopped encoding...");
                progress.setProgress(0.);
                final Alert alert = new Alert(Alert.AlertType.WARNING, expanded.getMessage(), ButtonType.OK);
                final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
                alert.showAndWait();
            });
        } catch (Exception except) {
            Platform.runLater(() -> {
                status.setText("Stopped encoding...");
                progress.setProgress(0.);
                final Alert alert = new Alert(Alert.AlertType.ERROR, "Critical error when Reading/Writing to file.", ButtonType.OK);
                final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
                alert.showAndWait();
            });
        }
    }
}
