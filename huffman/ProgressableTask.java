package huffman;

public abstract class ProgressableTask  {
    protected interface RunnableTask {
        void run() throws Exception;
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

        public void run() throws Exception {
            task.run();
        }
    }

    public abstract TaskPhase[] getPhases();

    public abstract void cleanup();
}
