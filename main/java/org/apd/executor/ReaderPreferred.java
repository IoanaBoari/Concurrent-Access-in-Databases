package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

public class ReaderPreferred implements LockTypeHandler {

    public EntryResult handle(StorageTask task, ReaderWriterSharedVars sharedVars, SharedDatabase sharedDatabase) {
        EntryResult result = null;
        int index = task.index();
        try {
            if (task.isWrite()) {
                // writers require exclusive access to the shared database
                sharedVars.readOrwrite[index].acquire(); // acquire lock for exclusive access to write
                try {
                    result = sharedDatabase.addData(index, task.data());
                } finally {
                    sharedVars.readOrwrite[index].release(); // release the lock after writing
                }
            } else {
                // readers can access the resource concurrently
                sharedVars.readers_lock[index].lock(); // protect modifications to readers variable
                try {
                    sharedVars.readers[index]++;
                    if (sharedVars.readers[index] == 1) {
                        // first reader locks the readOrwrite semaphore to block writers
                        sharedVars.readOrwrite[index].acquire();
                    }
                } finally {
                    sharedVars.readers_lock[index].unlock(); // release the lock after updating readers
                }

                try {
                    result = sharedDatabase.getData(index);
                } finally {
                    sharedVars.readers_lock[index].lock();
                    try {
                        sharedVars.readers[index]--;
                        if (sharedVars.readers[index] == 0) {
                            // last reader releases the readOrwrite semaphore to allow writers
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
