package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

public class WriterPreferred1 implements LockTypeHandler {

    public EntryResult handle(StorageTask task, ReaderWriterSharedVars sharedVars, SharedDatabase sharedDatabase) {
        EntryResult result = null;
        int index = task.index();
        try {
            if (task.isWrite()) {
                sharedVars.writers_lock[index].lock(); // lock to modify the writers variable
                try {
                    sharedVars.writers[index]++;
                } finally {
                    sharedVars.writers_lock[index].unlock(); // unlock after updating the variable
                }

                sharedVars.readOrwrite[index].acquire(); // try to gain access to the database in order to write
                try {
                    result = sharedDatabase.addData(index, task.data());
                } finally {
                    sharedVars.readOrwrite[index].release(); // free the resource after writing

                    sharedVars.writers_lock[index].lock();
                    try {
                        sharedVars.writers[index]--;
                    } finally {
                        sharedVars.writers_lock[index].unlock();
                    }
                }
            } else {
                while (true) {
                    // wait until there are no writers waiting or writing
                    sharedVars.writers_lock[index].lock(); // lock to check the writers variable
                    try {
                        if (sharedVars.writers[index] == 0) {
                            // all writers have finished so there is no need to wait
                            break;
                        }
                    } finally {
                        sharedVars.writers_lock[index].unlock(); // unlock after checking the condition
                    }
                }

                sharedVars.readers_lock[index].lock();
                try {
                    sharedVars.readers[index]++;
                    if (sharedVars.readers[index] == 1) {
                        // first reader has to block writers until finishing reading
                        sharedVars.readOrwrite[index].acquire();
                    }
                } finally {
                    sharedVars.readers_lock[index].unlock();
                }

                try {
                    result = sharedDatabase.getData(index);
                } finally {
                    sharedVars.readers_lock[index].lock();
                    try {
                        sharedVars.readers[index]--;
                        if (sharedVars.readers[index] == 0) {
                            // last reader has to release the lock to allow writers to write
                            sharedVars.readOrwrite[index].release();
                        }
                    } finally {
                        sharedVars.readers_lock[index].unlock();
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
