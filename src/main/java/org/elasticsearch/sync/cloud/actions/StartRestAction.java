package org.elasticsearch.sync.cloud.actions;


import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.messages.ErrorResponseMessage;
import org.elasticsearch.sync.cloud.messages.StartResponseMessage;
import org.elasticsearch.sync.cloud.start.StartAtSinkThread;
import org.elasticsearch.sync.cloud.start.StartAtSourceThread;
import org.elasticsearch.sync.cloud.start.StartInfo;
import org.elasticsearch.sync.cloud.utils.IndexInfo;
import org.elasticsearch.sync.cloud.utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.POST;

public class StartRestAction extends BaseRestHandler {

    private final Logger logger = ESLoggerFactory.getLogger(StartRestAction.class);

    @Inject
    public StartRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/cloudsync/start", this);
    }

    @Override
    protected BaseRestHandler.RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        try {
            //read and validate request.
            StartInfo startInfo = new StartInfo(restRequest.content().utf8ToString());
            logger.info("cloud-sync- source start.");

            ElasticClient elastic = new ElasticClient(client);

            //todo: needs change once aws-s3 is supported   .
            elastic.isPluginLoaded("repository-gcs");

            if (StartInfo.SOURCE.equals(startInfo.getMode())) {
                sourceActions(startInfo, elastic);
            } else if (StartInfo.SINK.equals(startInfo.getMode())) {
                sinkActions(startInfo, elastic);
            }
        } catch (Exception ex){
            logger.error("cloud-sync start action failed with error: ",ex);
            return channel -> {
                ErrorResponseMessage message = new ErrorResponseMessage(ex.getMessage());
                XContentBuilder builder = channel.newBuilder().startObject();
                message.toXContent(builder, restRequest);
                builder.endObject();
                channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, builder));
            };
        }

        return channel -> {
            logger.info("cloud-sync start action accepted.");
            StartResponseMessage message = new StartResponseMessage();
            XContentBuilder builder = channel.newBuilder().startObject();
            message.toXContent(builder, restRequest);
            builder.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.ACCEPTED, builder));
        };
    }

    private void sourceActions(StartInfo startInfo, ElasticClient elastic) throws IOException {
        elastic.isPluginLoaded("repository-gcs");
        handleStartState(elastic,startInfo.getIndices());

        StartAtSourceThread sourceThread = new StartAtSourceThread(elastic, startInfo);
        new Thread(sourceThread).start();
    }

    private void sinkActions(StartInfo startInfo, ElasticClient elastic) throws IOException {
        StartAtSinkThread sinkThread = new StartAtSinkThread(elastic,startInfo);
        new Thread(sinkThread).start();
    }

    private void handleStartState(ElasticClient elastic,final String indicesPattern) throws IOException {
        String prevState = elastic.readState();
        if(!prevState.isEmpty()) {
            List<IndexInfo> list = Utils.sortAndFilter(Utils.toSnapshots(prevState), IndexInfo.State.READY);
            if(list.size() > 0 ) {
                logger.info("cloud-sync found valid previous run state. Ignoring input indices pattern. " +
                        "If you want to run with new indices list, delete .cloudsync index. `curl -XDELETE localhost:9200/.cloudsync` ");
                return;
            }
        }
        Map<String, Long> indices = elastic.getIndices(indicesPattern);
        logger.info("cloud-sync indices count: "+indices.size());
        String snapshotsJson = Utils.toJson(indices);
        if(!elastic.createStateIndex()){
            logger.error("cloud-sync failed to create .cloudsync index.");
            throw new IOException("Failed to create .cloudsync index");
        }

        if(!elastic.writeState(snapshotsJson)){
            logger.error("cloud-sync failed to create .cloudsync index.");
            throw new IOException("Failed to write state to .cloudsync index");
        }
    }
}
