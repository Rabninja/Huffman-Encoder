package huffman;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.File;

public abstract class ProgressableTask implements Runnable {
    protected ProgressBar progress;
    protected Label status;
    protected File file;

    protected interface RunnableTask {
        void run() throws Exception;
    }

    protected class TaskPiece {
        public String message;
        public Double progress;
        public RunnableTask task;

        public TaskPiece(String message, Double progress, RunnableTask task) {
            this.message = message;
            this.progress = progress;
            this.task = task;
        }
    }

    public ProgressableTask(ProgressBar progress, Label status, File file) {
        this.progress = progress;
        this.status = status;
        this.file = file;
    }
}
