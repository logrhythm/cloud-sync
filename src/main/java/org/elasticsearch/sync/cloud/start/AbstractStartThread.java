package org.elasticsearch.sync.cloud.start;

import org.elasticsearch.sync.cloud.elastic.ElasticClient;

import java.io.IOException;

public abstract class AbstractStartThread {

    protected final  String repository = "cloudsync_backup";
    protected final String snapshotNamePrefix = "snapshot_";
    protected final StartInfo startInfo;
    protected final ElasticClient client;

    public AbstractStartThread(final ElasticClient client, final StartInfo startInfo){
        this.startInfo = startInfo;
        this.client = client;
    }

    protected void createRepository() throws IOException {
        boolean hasRepository = client.hasRepository(repository);
        if(!hasRepository){
            boolean ack = client.createRepository(startInfo.getStore(),repository,startInfo.getLocation());
            if(!ack){
                throw new IOException("Failed to create repository.");
            }
        }
    }

}
