package huffman;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class DecodeFile extends ProgressableTask {
    private HuffmanEncoder encoder = new HuffmanEncoder();
    private ObservableList<String> counts;
    private ObservableList<String> encodings;
    private String destination;

    public static class InvalidHuffman extends RuntimeException {
        InvalidHuffman() {
            super("Error reading requested file, file has an invalid signature that doesn't match huffman encoding.");
        }
    }

    public DecodeFile(String destination, ObservableList<String> counts, ObservableList<String> encodings, ProgressBar progress, Label status, File file) {
        super(progress, status, file);
        this.destination = destination;
        this.counts = counts;
        this.encodings = encodings;
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
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file.getPath());
            BufferedInputStream input = new BufferedInputStream(fin);
            final TaskPiece[] stages = new TaskPiece[] {
                new TaskPiece("Verifying huffman file...", 0., (RunnableTask) () -> {
                    if (!encoder.validateHuffman(input)) {
                        throw new InvalidHuffman();
                    }
                }),
                new TaskPiece("Extracting huffman tree...", 0.1, (RunnableTask) () -> {
                    encoder.extractEncodings(input);
                }),
                new TaskPiece("Update character encoding tab...", 0.5, (RunnableTask) () -> {
                    Platform.runLater(() -> {
                        encodings.clear();

                        for (var encoding : encoder.getEncodings()) {
                            encodings.add(encoding.toString());
                        }
                    });
                }),
                new TaskPiece("Decoding file...", .7, (RunnableTask) () -> {
                    encoder.decodeTo(input, destination);
                }),
                new TaskPiece("Finished decoding file...", 1., (RunnableTask) () -> {

                })
            };

            for (var stage : stages) {
                update(stage.message, stage.progress);
                stage.task.run();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (InvalidHuffman huff) {
            Platform.runLater(() -> {
                status.setText("Stopped decoding...");
                progress.setProgress(0.);
                final Alert alert = new Alert(Alert.AlertType.ERROR, huff.getMessage(), ButtonType.OK);
                final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
                alert.showAndWait();
            });
        } catch (Exception except) {
            Platform.runLater(() -> {
                status.setText("Stopped decoding...");
                progress.setProgress(0.);
                final Alert alert = new Alert(Alert.AlertType.ERROR, "File was ill-formed, missing prepended tree or padding.", ButtonType.OK);
                final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
                alert.showAndWait();
            });
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
