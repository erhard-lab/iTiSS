package gedi.utils.multithreading;

public interface RunnableFinishedListener {
    void notifyRunnableFinished(final Runnable runnable);
}
