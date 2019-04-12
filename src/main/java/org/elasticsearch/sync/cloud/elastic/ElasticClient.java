package org.elasticsearch.sync.cloud.elastic;


import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryResponse;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import org.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.plugins.PluginInfo;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.snapshots.SnapshotInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of Elastic utils
 */
public class ElasticClient {

    private NodeClient client;
    private final String stateIndex = ".cloudsync";


    public ElasticClient(NodeClient client) {
        this.client = client;
    }

    private boolean indexExists()  {
        ClusterStateRequestBuilder request = client.admin().cluster().prepareState();
        ClusterStateResponse response = request.execute().actionGet();
        return response.getState().metaData().hasIndex(stateIndex);
    }

    /**
     * Create .cloudsync index to store the progress/state.
     * @return
     */
    public boolean createStateIndex(){
        if(indexExists())
            return true;

        CreateIndexRequest request = new CreateIndexRequest(stateIndex);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0));

        return client.admin().indices().create(request).actionGet().isAcknowledged();
    }

    /**
     * Writes to the .cloudsync index.
     * @param json
     * @return
     */
    public boolean writeState(final String json){
        IndexResponse response = client.prepareIndex(stateIndex, "doc","1")
                .setSource(json, XContentType.JSON)
                .execute().actionGet();
        return (response.status().getStatus() == RestStatus.CREATED.getStatus()) ;
    }

    /**
     * Returns the lastest version of the stored doc.
     * @return
     */
    public String readState(){
        if(!indexExists())
            return "";
        return client.prepareGet(stateIndex, "doc", "1").get().getSourceAsString();
    }

    /**
     * Get indices matching the pattern.
     * @param pattern
     * @return
     */
    public Map<String, Long> getIndices(String pattern) {
        IndicesStatsResponse r = client.admin().indices().stats(new IndicesStatsRequest().indices(pattern)).actionGet();
        Map<String, IndexStats> indices = r.getIndices();
        Map<String, Long> filteredIndices = new HashMap<>();
        for (String index : indices.keySet()) {
            filteredIndices.put(index, indices.get(index).getTotal().store.sizeInBytes());
        }
        return filteredIndices;
    }


    /**
     * Request to take a snapshot.
     * @param index
     * @return
     */
    public void takeSnapshot(final String repository, final String snapshotName, final String index) throws IOException {
        IndicesOptions indicesOptions = IndicesOptions.fromOptions(true, true,
                true, false, IndicesOptions.lenientExpandOpen());

        CreateSnapshotRequest createSnapshotRequest = new CreateSnapshotRequest(repository, snapshotName)
                .indices(index)
                .includeGlobalState(false)
                .indicesOptions(indicesOptions);

        CreateSnapshotResponse createSnapshotResponse = client.admin().cluster().createSnapshot(createSnapshotRequest).actionGet();
        if (RestStatus.ACCEPTED.getStatus() != createSnapshotResponse.status().getStatus()) {
            throw new IOException("Snapshot creation failed.");
        }
    }

    /**
     * Checks - if repository is present.
     * @return
     */
    public boolean hasRepository(final String repository) {
        try {
            VerifyRepositoryRequest request = new VerifyRepositoryRequest().name(repository);
            VerifyRepositoryResponse response = client.admin().cluster().verifyRepository(request).actionGet();
            if (response.getNodes() == null || response.getNodes().length == 0)
                return false;
        } catch (RepositoryMissingException ex) {
            return false;
        }
        return true;
    }

    /**
     * Create repository.
     *
     * @param type
     * @param location if type is fs its folder , if type is gcs its bucket name.
     * @return
     */
    public boolean createRepository(final String type, final String repository, final String location) {
        Map<String, Object> settings = new HashMap<>();

        if("fs".equals(type)) {
            settings.put("location", location);
            settings.put("compress", "true");
        } else if("gcs".equals(type)){
            settings.put("bucket", location);
        }
        //todo: support aws-s3

        PutRepositoryRequest request = new PutRepositoryRequest()
                .type(type)
                .name(repository)
                .settings(settings);

        return client.admin().cluster().putRepository(request).actionGet().isAcknowledged();
    }

    /**
     * Deletes repository.
     * @param repository
     * @return
     */
    public boolean deleteRepository(final String repository) {
        DeleteRepositoryRequest request = new DeleteRepositoryRequest().name(repository);
        return client.admin().cluster().deleteRepository(request).actionGet().isAcknowledged();
    }

    /**
     * Deletes snapshot
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean deleteSnapshot(final String repository, final String snapshot) {
        DeleteSnapshotRequest request = new DeleteSnapshotRequest().repository(repository).snapshot(snapshot);
        return client.admin().cluster().deleteSnapshot(request).actionGet().isAcknowledged();
    }

    /**
     * Restores snapshot.
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean restoreSnapshot(final String repository, final String snapshot) {
        RestoreSnapshotRequest request = new RestoreSnapshotRequest().repository(repository).snapshot(snapshot);
        RestoreSnapshotResponse response = client.admin().cluster().restoreSnapshot(request).actionGet();
        if (RestStatus.ACCEPTED.getStatus() == response.status().getStatus()) {
            return true;
        }
        return false;
    }

    /**
     * List all snapshots in the repository
     * @param repository
     * @return
     */
    public List<String> listSnapshots(final String repository){
        String[] ss = {"_all"};
        GetSnapshotsRequest request = new GetSnapshotsRequest().repository(repository).snapshots(ss);
        GetSnapshotsResponse response = client.admin().cluster().getSnapshots(request).actionGet();
        List<String> list = new ArrayList<>();
        List<SnapshotInfo> ssInfo = response.getSnapshots();
        for(SnapshotInfo info : ssInfo){
            list.add(info.snapshotId().getName());
        }
        return list;
    }
    /**
     * @param repository
     * @param snapshot
     * @return
     */
    public boolean snapshotsStatus(final String repository, final String snapshot) {
        String[] ss = {snapshot};
        SnapshotsStatusRequest request = new SnapshotsStatusRequest().repository(repository).snapshots(ss);
        SnapshotsStatusResponse response = client.admin().cluster().snapshotsStatus(request).actionGet();
        return (response.getSnapshots().size() == 0 ) ? false : response.getSnapshots().get(0).getState().completed();
    }

    /**
     *
     * @param repository
     * @param snapshot
     * @return
     */
    public int snapshotsCount(final String repository, final String snapshot) {
        String[] ss = {snapshot};
        GetSnapshotsRequest request = new GetSnapshotsRequest().repository(repository).snapshots(ss);
        GetSnapshotsResponse response = client.admin().cluster().getSnapshots(request).actionGet();
        return response.getSnapshots().size();
    }

    /**
     * Blocks till the cluster turns to green.
     * @param index
     */
    public void waitForIndexGreenStatus(String index) {
        while (true) {
            ClusterHealthResponse response = client.admin().cluster().prepareHealth(index).get();
            ClusterHealthStatus status = response.getIndices().get(index).getStatus();
            if (!status.equals(ClusterHealthStatus.GREEN)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //ignore
                }
            } else {
                break;
            }
        }
    }

    public void isPluginLoaded(final String name) throws IOException {
        NodesInfoResponse response = client.admin().cluster().prepareNodesInfo().setPlugins(true).get();
        boolean pluginFound = false;
        for (NodeInfo nodeInfo : response.getNodes()) {
            for (PluginInfo pluginInfo : nodeInfo.getPlugins().getPluginInfos()) {
                if (pluginInfo.getName().equals(name)) {
                    pluginFound = true;
                    break;
                }
            }
        }
        if(!pluginFound){
            throw new IOException("Plugin not found: "+name);
        }
    }
}