package com.frozeninferno.server.utils;

import java.nio.file.Path;

/**
 * Interface for listening to WatchDir events
 */
public interface WatchDirListener {
    void entryCreated(String uuid, Path child);
    void entryDeleted(String uuid, Path child);
    void entryModified(String uuid, Path child);
}
