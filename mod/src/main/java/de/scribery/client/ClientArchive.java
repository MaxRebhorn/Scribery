package de.scribery.client;

import de.scribery.ArchiveData;
import de.scribery.core.ArchiveProgress;

/**
 * Die Sammlung des Spielers im Manuscript Archive, wie der Server sie zuletzt geschickt hat. Der Client zeigt sie nur an;
 * eingetragen wird auf dem Server (ArchiveService). {@link #version()} zaehlt hoch, wenn sich etwas geaendert hat.
 */
public final class ClientArchive {
    private static ArchiveData data = ArchiveData.EMPTY;
    private static int version;

    private ClientArchive() {}

    public static void set(ArchiveData fresh) {
        data = fresh;
        version++;
    }

    public static ArchiveProgress get(String manuscriptId) {
        return data.get(manuscriptId);
    }

    public static ArchiveData data() {
        return data;
    }

    /** Aendert sich bei jedem neuen Stand, damit der Screen seine Zwischenspeicher neu aufbaut. */
    public static int version() {
        return version;
    }
}
