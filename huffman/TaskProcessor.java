package huffman;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TaskProcessor implements Runnable {
    private final ProgressBar progress;
    private final Label status;
    private final ProgressableTask task;

    private static class PhaseProgress {
        private Integer active;
        private AtomicReference<Double> progress;
        private ProgressableTask.TaskPhase[] phases;

        private static class Data {
            private Integer active;
            private Double progress;

            public Data(Integer active, Double progress) {
                this.active = active;
                this.progress = progress;
            }

            public int getActive() {
                return active;
            }

            public double getProgress() {
                return progress;
            }
        }

        public PhaseProgress(AtomicReference<Double> progress, ProgressableTask.TaskPhase[] phases) {
            this.active = 0;
            this.progress = progress;
            this.phases = phases;
        }

        synchronized public Data get() {
            return new Data(active, progress.get());
        }

        synchronized public void increment() {
            ++active;
            progress.set(0.0);
        }

        public ProgressableTask.TaskPhase[] getPhases() {
            return phases;
        }
    }

    private final AtomicReference<Double> completion = new AtomicReference<>(0.);
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private PhaseProgress forward;

    public TaskProcessor(ProgressBar progress, Label status, ProgressableTask task) {
        this.progress = progress;
        this.status = status;
        this.task = task;
    }

    @Override
    public void run() {
        try {
            final var phases = task.getPhases();
            forward = new PhaseProgress(completion, phases);
            final Thread polling = new Thread(this::pollUpdates);
            polling.start();

            for (var phase : phases) {
                updateVisuals(phase.message, phase.progress);
                phase.run(completion);

                try {
                    Thread.sleep(67);
                } catch (InterruptedException ignored) {

                }
                forward.increment();
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
            alive.set(false);
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

    private void pollUpdates() {
        while (alive.get()) {
            final var packet = forward.get();
            final var phases = forward.getPhases();

            if (packet.active + 1 < phases.length) {
                final double baseline = phases[packet.active].progress;
                final double nextBaseline = phases[packet.active + 1].progress;
                final double completion = baseline + (nextBaseline - baseline) * packet.getProgress();
                Platform.runLater(() -> {
                    progress.setProgress(completion);
                });
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {

            }
        }
    }
}
