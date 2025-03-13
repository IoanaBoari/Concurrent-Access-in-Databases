package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

public class WriterPreferred2 implements LockTypeHandler {

    public EntryResult handle(StorageTask task, ReaderWriterSharedVars sharedVars, SharedDatabase sharedDatabase) {
        EntryResult result = null;
        int index = task.index();
        try {
            if (task.isWrite()) {
                synchronized (sharedVars.monitors[index]) {
                    sharedVars.writers[index]++; // increment the number of writers
                    // wait until there are no readers or the resource is free
                    while (sharedVars.readers[index] > 0 || sharedVars.readOrwrite[index].availablePermits() == 0) {
                        try {
                            sharedVars.monitors[index].wait(); // waits for resource availability
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.err.println("Thread was interrupted: " + e.getMessage());
                            return null;
                        }
                    }
                }

                sharedVars.readOrwrite[index].acquire(); // gain exclusive access to the specified index in database for writing

                try {
                    result = sharedDatabase.addData(index, task.data());
                } finally {
                    synchronized (sharedVars.monitors[index]) {
                        sharedVars.writers[index]--; // decrement the count of writers
                        sharedVars.readOrwrite[index].release(); // release the specified index in database
                        sharedVars.monitors[index].notifyAll(); // notify waiting threads
                    }
                }
            } else {
                synchronized (sharedVars.monitors[index]) {
                    // Wait until no writers are waiting or writing
                    while (sharedVars.writers[index] > 0) {
                        try {
                            sharedVars.monitors[index].wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.err.println("Thread was interrupted: " + e.getMessage());
                            return null;
                        }
                    }
                }
                synchronized (sharedVars.monitors[index]) {
                    sharedVars.readers[index]++; // increment the count of readers
                    if (sharedVars.readers[index] == 1) {
                        // first reader blocks writers by acquiring the lock
                        sharedVars.readOrwrite[index].acquire();
                    }
                }

                try {
                    result = sharedDatabase.getData(index);
                } finally {
                    synchronized (sharedVars.monitors[index]) {
                        sharedVars.readers[index]--;
                        if (sharedVars.readers[index] == 0) {
                            // last reader releases the lock for writers
                            sharedVars.readOrwrite[index].release();
                        }
                        // notify the writers if there are any waiting for specified index in database
                        if (sharedVars.writers[index] > 0) {
                            sharedVars.monitors[index].notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            // manage thread interruption exception
            Thread.currentThread().interrupt();
            System.err.println("Thread was interrupted: " + e.getMessage());
        }

        return result;
    }
}
