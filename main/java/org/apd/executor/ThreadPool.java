package org.apd.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {
    private final BlockingQueue<Runnable> queueForTasks;
    private final List<MyThread> myThreads;
    private boolean isShutdown = false;

    public ThreadPool(int numberOfThreads) {
        queueForTasks = new LinkedBlockingQueue<>();
        myThreads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            MyThread newThread = new MyThread(queueForTasks);
            myThreads.add(newThread);
            newThread.start();
        }
    }

    // submits a new task to the thread pool
    public void submit(Runnable task) throws InterruptedException {
        synchronized (this) {
            if (isShutdown) {
                throw new IllegalStateException("ThreadPool is shut down. Cannot accept new tasks.");
            }
        }
        queueForTasks.put(task);
    }

    // shuts down the thread pool by adding PoisonPill tasks
    public void shutdown() {
        synchronized (this) {
            isShutdown = true;
        }
        for (int i = 0; i < myThreads.size(); i++) {
            try {
                queueForTasks.put(new PoisonPill());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // waits for all worker threads to finish
    public void awaitTermination() throws InterruptedException {
        for (MyThread thread : myThreads) {
            thread.join();
        }
    }

    private static class MyThread extends Thread {
        private final BlockingQueue<Runnable> taskQueue;

        public MyThread(BlockingQueue<Runnable> taskQueue) {
            this.taskQueue = taskQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable task = taskQueue.take();
                    if (task instanceof PoisonPill) {
                        break;
                    }
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        System.err.println("Task execution error: " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted: " + Thread.currentThread().getName());
            }
        }
    }

    // special task to signal worker threads to finish
    private static class PoisonPill implements Runnable {
        @Override
        public void run() {
            // empty method for signaling the threads to stop
        }
    }
}
