package org.elasticsearch.sync.cloud.messages;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class ErrorResponseMessage implements ToXContent {

    private final String msg;

    public ErrorResponseMessage(String msg) {
        this.msg = msg;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("failed", msg);
    }
}
