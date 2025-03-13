package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

public interface LockTypeHandler {
    EntryResult handle(StorageTask task, ReaderWriterSharedVars sharedVars, SharedDatabase sharedDatabase);
}
