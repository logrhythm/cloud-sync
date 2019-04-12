package org.elasticsearch.sync.cloud.messages;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class StartResponseMessage implements ToXContent {

    public StartResponseMessage() {

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.field("acknowledged", true);
    }
}
