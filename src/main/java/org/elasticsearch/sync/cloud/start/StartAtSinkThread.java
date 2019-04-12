package org.elasticsearch.sync.cloud.start;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;

import java.io.IOException;
import java.util.List;

/**
 * Background thread to restore snapshots. Runs only on the sink (cluster).
 *
 */
public class StartAtSinkThread extends AbstractStartThread implements Runnable {

    private final Logger logger = ESLoggerFactory.getLogger(StartAtSinkThread.class);
    private static final int sleepIntervalMSecs = 10000;

    public StartAtSinkThread(final ElasticClient client, final StartInfo startInfo){
        super(client,startInfo);
    }

    @Override
    public void run() {
        try {
            createRepository();
        } catch (IOException ex){
            logger.error("cloud-sync failed to create repository on the sink cluster. Exiting the snapshot restore thread.");
            return;
        }
        logger.info("cloud-sync starting snapshot restore thread.");

        while (true) {
            try {
                List<String> snapshotNames = client.listSnapshots(repository);
                for(String ssName : snapshotNames){
                    String index = getIndexName(ssName);

                    boolean ack = client.restoreSnapshot(repository, ssName);
                    if(ack) {
                        client.waitForIndexGreenStatus(index);
                        client.deleteSnapshot(repository, ssName);
                    }
                }
                Thread.sleep(sleepIntervalMSecs);
            } catch (Exception ex) {
                logger.error("cloud-sync Restore of snapshot failed !!!",ex);
            }
        }
    }

    private String getIndexName(String snapshot){
        return snapshot.substring(snapshotNamePrefix.length(),snapshot.length());
    }

}