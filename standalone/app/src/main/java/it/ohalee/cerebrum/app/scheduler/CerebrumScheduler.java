package it.ohalee.cerebrum.app.scheduler;

import it.ohalee.cerebrum.app.Logger;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CerebrumScheduler {

    private static final int PARALLELISM = 16;

    private final ScheduledThreadPoolExecutor scheduler;
    private final ForkJoinPool worker;

    public CerebrumScheduler() {
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("cerebrum-scheduler");
            return thread;
        });
        this.scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.worker = new ForkJoinPool(PARALLELISM, new WorkerThreadFactory(), new ExceptionHandler(), false);
    }

    public Executor async() {
        return this.worker;
    }

    public void execute(Runnable runnable) {
        async().execute(runnable);
    }

    public void shutdownScheduler() {
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                Logger.severe("Timed out waiting for the LuckPerms scheduler to terminate");
                reportRunningTasks(thread -> thread.getName().equals("luckperms-scheduler"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdownExecutor() {
        this.worker.shutdown();
        try {
            if (!this.worker.awaitTermination(1, TimeUnit.MINUTES)) {
                Logger.severe("Timed out waiting for the LuckPerms worker thread pool to terminate");
                reportRunningTasks(thread -> thread.getName().startsWith("luckperms-worker-"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reportRunningTasks(Predicate<Thread> predicate) {
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (predicate.test(thread)) {
                Logger.warn("Thread " + thread.getName() + " is blocked, and may be the reason for the slow shutdown!\n" +
                        Arrays.stream(stack).map(el -> "  " + el).collect(Collectors.joining("\n"))
                );
            }
        });
    }

    private static final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setDaemon(true);
            thread.setName("cerebrum-worker-" + COUNT.getAndIncrement());
            return thread;
        }
    }

    private static final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Logger.warn("Thread " + t.getName() + " threw an uncaught exception", e);
        }
    }

}
