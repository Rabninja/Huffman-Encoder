package huffman;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class TaskProcessor implements Runnable {
    private ProgressBar progress;
    private Label status;
    private ProgressableTask task;

    public TaskProcessor(ProgressBar progress, Label status, ProgressableTask task) {
        this.progress = progress;
        this.status = status;
        this.task = task;
    }

    @Override
    public void run() {
        final var phases = task.getPhases();

        try {
            for (var phase : phases) {
                updateVisuals(phase.message, phase.progress);
                phase.run();

                try {
                    Thread.sleep(67);
                } catch (InterruptedException ignored) {

                }
            }
        } catch (Exception except) {
            Platform.runLater(() -> {
                status.setText("Stopped decoding...");
                progress.setProgress(0.);
                final Alert alert = new Alert(Alert.AlertType.ERROR, except.getMessage(), ButtonType.OK);
                final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
                alert.showAndWait();
            });
        } finally {
            task.cleanup();
        }
    }

    private void updateVisuals(final String string, final Double val) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                status.setText(string);
                progress.setProgress(val);
            }
        });
    }
}
