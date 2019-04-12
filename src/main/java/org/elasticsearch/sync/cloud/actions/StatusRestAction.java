package org.elasticsearch.sync.cloud.actions;


import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.messages.ErrorResponseMessage;
import org.elasticsearch.sync.cloud.messages.StatusResponseMessage;
import org.elasticsearch.sync.cloud.status.StatusInfo;
import org.elasticsearch.sync.cloud.utils.IndexInfo;
import org.elasticsearch.sync.cloud.utils.Utils;

import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class StatusRestAction extends BaseRestHandler {

   protected final  String repository = "cloudsync_backup";

    @Inject
    public StatusRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/cloudsync/status", this);
    }

    @Override
    protected BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {
        // todo: .cloudsync index is created and updated only on source cluster.
        // that makes this api valid on source side. this could be fixed by replicating .cloudsync index to
        // sink cluster.


        ElasticClient elastic = new ElasticClient(client);
        String json = elastic.readState();
        if(json.isEmpty()){
            return channel -> {
                ErrorResponseMessage message = new ErrorResponseMessage("No valid cloudsync state found. This api is " +
                        "valid only on the source cluster.");
                XContentBuilder builder = channel.newBuilder().startObject();
                message.toXContent(builder, restRequest);
                builder.endObject();
                channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, builder));
            };
        }


        return channel -> {
            StatusResponseMessage message = new StatusResponseMessage(calcStatus(elastic,json));
            XContentBuilder builder = channel.newBuilder();
            builder.startObject();
            message.toXContent(builder, restRequest);
            builder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
        };
    }

    private StatusInfo calcStatus(ElasticClient elastic, String json){
        StatusInfo statusInfo = new StatusInfo();
        List<IndexInfo> indices = Utils.toSnapshots(json);
        List<String> snapShots = elastic.listSnapshots(repository);

        statusInfo.totalIndices = indices.size();
        int currentSnapShoted = 0;

        for(IndexInfo index :indices){
            statusInfo.totalSizeInBytes += index.getSizeInBytes();
            if(index.getState() == IndexInfo.State.READY){
                statusInfo.pendingIndicesToSnapshot++;
                statusInfo.totalPendingInBytes += index.getSizeInBytes();
            }
            if(index.getState() == IndexInfo.State.SNAPSHOTED){
                currentSnapShoted++;
            }
        }
        statusInfo.pendingIndicesToRestore = statusInfo.totalIndices - statusInfo.pendingIndicesToSnapshot;
        statusInfo.completedIndices = currentSnapShoted - snapShots.size();
        return statusInfo;
    }
}
