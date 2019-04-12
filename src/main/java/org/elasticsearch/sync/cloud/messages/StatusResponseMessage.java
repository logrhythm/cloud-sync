package org.elasticsearch.sync.cloud.messages;

import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.sync.cloud.status.StatusInfo;

import java.io.IOException;

public class StatusResponseMessage implements ToXContent {

    private final StatusInfo info;

    public StatusResponseMessage(StatusInfo info) {
        this.info = info;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("totalIndices", info.totalIndices)
                .field("completedIndices", info.completedIndices)
                .field("pendingIndicesToSnapshot", info.pendingIndicesToSnapshot)
                .field("pendingIndicesToRestore", info.pendingIndicesToRestore)
                .field("totalSize", new ByteSizeValue(info.totalSizeInBytes))
                .field("totalPendingSize", new ByteSizeValue(info.totalPendingInBytes));
    }
}
