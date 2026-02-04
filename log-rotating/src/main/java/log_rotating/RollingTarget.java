package log_rotating;

import java.io.File;

public class RollingTarget {

    private final String name;
    private final File activeLog;
    private final File archivedDir;
    private long lastSize = 0;

    public RollingTarget(String name, String activePath, String archivedPath) {
        this.name = name;
        this.activeLog = new File(activePath);
        this.archivedDir = new File(archivedPath);
    }

    public String getName() {
        return name;
    }

    public File getActiveLog() {
        return activeLog;
    }

    public File getArchivedDir() {
        return archivedDir;
    }

    public long getLastSize() {
        return lastSize;
    }

    public void updateLastSize(long currentSize) {
        this.lastSize = currentSize;
    }
}
