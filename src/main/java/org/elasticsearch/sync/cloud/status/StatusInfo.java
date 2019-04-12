package org.elasticsearch.sync.cloud.status;

public class StatusInfo {
    public int totalIndices;
    public int completedIndices;
    public int pendingIndicesToSnapshot;
    public int pendingIndicesToRestore;

    public long totalSizeInBytes;
    public long totalPendingInBytes;

}
