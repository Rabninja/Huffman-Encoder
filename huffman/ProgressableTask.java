package huffman;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ProgressableTask  {
    protected interface RunnableTask {
        void run(AtomicReference<Double> progress) throws Exception;
    }

    protected class TaskPhase implements RunnableTask {
        public String message;
        public Double progress;
        public RunnableTask task;

        public TaskPhase(String message, Double progress, RunnableTask task) {
            this.message = message;
            this.progress = progress;
            this.task = task;
        }

        public void run(AtomicReference<Double> progress) throws Exception {
            task.run(progress);
        }
    }

    public abstract TaskPhase[] getPhases();

    public abstract void cleanup();
}
