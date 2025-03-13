package org.apd.executor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterSharedVars {
    final int[] readers;
    final int[] writers;
    final ReentrantLock[] readers_lock; // lock to protect access to readers variable
    final ReentrantLock[] writers_lock; // lock to protect access to writers variable
    final Semaphore[] readOrwrite; // semaphore to control access to the shared database
    final Object[] monitors; // java monitors for synchronization

    // Initialize the shared variables for access control
    public ReaderWriterSharedVars(int storageSize) {
        readers = new int[storageSize];
        writers = new int[storageSize];
        readers_lock = new ReentrantLock[storageSize];
        writers_lock = new ReentrantLock[storageSize];
        readOrwrite = new Semaphore[storageSize];
        monitors = new Object[storageSize];

        for (int i = 0; i < storageSize; i++) {
            readers[i] = 0;
            writers[i] = 0;
            readers_lock[i] = new ReentrantLock(true);
            writers_lock[i] = new ReentrantLock(true);
            readOrwrite[i] = new Semaphore(1, true);
            monitors[i] = new Object();
        }
    }
}
