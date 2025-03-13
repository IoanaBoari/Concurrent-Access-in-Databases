package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* DO NOT MODIFY THE METHODS SIGNATURES */
public class TaskExecutor {
    private final SharedDatabase sharedDatabase;

    public TaskExecutor(int storageSize, int blockSize, long readDuration, long writeDuration) {
        sharedDatabase = new SharedDatabase(storageSize, blockSize, readDuration, writeDuration);
    }

    public List<EntryResult> ExecuteWork(int numberOfThreads, List<StorageTask> tasks, LockType lockType) {
        List<EntryResult> results = Collections.synchronizedList(new ArrayList<>());
        // initialize threadPool with the given number of threads
        ThreadPool threadPool = new ThreadPool(numberOfThreads);
        // shared variables for synchronization between threads
        ReaderWriterSharedVars sharedVars = new ReaderWriterSharedVars(sharedDatabase.getSize());

        LockTypeHandler lockTypeHandler = getLockHandler(lockType);
        for (StorageTask task : tasks) {
            try {
                threadPool.submit(() -> {
                    try {
                        // process the task and collect the result
                        EntryResult result = lockTypeHandler.handle(task, sharedVars, sharedDatabase);
                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Task submission interrupted: " + e.getMessage());
            }
        }
        // signal the threadPool to end execution when all tasks are completed
        threadPool.shutdown();
        try {
            // wait for all threads to finish
            threadPool.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ThreadPool termination interrupted: " + e.getMessage());
        }

        return results;
    }

    // select the appropriate handler based on LockType in order to use the right implementation
    private LockTypeHandler getLockHandler(LockType lockType) {
        return switch (lockType) {
            case ReaderPreferred -> new ReaderPreferred();
            case WriterPreferred1 -> new WriterPreferred1();
            case WriterPreferred2 -> new WriterPreferred2();
            default -> throw new IllegalArgumentException("Unknown LockType: " + lockType);
        };
    }

    public List<EntryResult> ExecuteWorkSerial(List<StorageTask> tasks) {
        var results = tasks.stream().map(task -> {
            try {
                if (task.isWrite()) {
                    return sharedDatabase.addData(task.index(), task.data());
                } else {
                    return sharedDatabase.getData(task.index());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return results.stream().toList();
    }
}
