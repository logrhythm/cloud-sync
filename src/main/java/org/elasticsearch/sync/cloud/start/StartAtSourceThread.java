package org.elasticsearch.sync.cloud.start;


import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.sync.cloud.elastic.ElasticClient;
import org.elasticsearch.sync.cloud.utils.IndexInfo;
import org.elasticsearch.sync.cloud.utils.Utils;

import java.io.IOException;
import java.util.List;


public class StartAtSourceThread extends AbstractStartThread implements Runnable  {

    private final Logger logger = ESLoggerFactory.getLogger(StartAtSourceThread.class);
    private static final int sleepIntervalMsecs = 10000;

    //'queue' size of the storage bucket. If current storage bucket size
    // is equal to maxAllowedSnapshots. source wont add new snapshot to the storage.
    private static final int maxAllowedSnapshots = 1;


    public StartAtSourceThread(final ElasticClient client, final StartInfo startInfo){
        super(client,startInfo);
    }


    @Override
    public void run() {
        try {
            createRepository();
        } catch (IOException ex){
            logger.error("cloud-sync failed to create repository on the source cluster. Exiting the snapshot thread.", ex);
            return;
        }
        logger.info("cloud-sync starting snapshots thread on source thread.");

        while (true) {
            try {
                blockOnPending();
                doSnapshot();
                Thread.sleep(sleepIntervalMsecs);
            } catch (IOException | InterruptedException ex) {
                logger.error("cloud-sync source thread failed to take snapshot.", ex);
                break;
            }
        }
    }

    /**
     * If more than the 'maxAllowedSnapshots' snapshots are present in the storage,
     * this blocks till those are cleared. Similar to a bounded queue.
     */
    private void blockOnPending() throws InterruptedException {
        while(true) {
            int count = client.snapshotsCount(repository, "_all");
            if (count >= maxAllowedSnapshots) {
                Thread.sleep(sleepIntervalMsecs);
            } else {
                break;
            }
        }
    }

    private void doSnapshot() throws IOException {
        String json = client.readState();
        List<IndexInfo> listSS = Utils.toSnapshots(json);
        List<IndexInfo> filtered = Utils.sortAndFilter(listSS, IndexInfo.State.READY);
        if(filtered.size() == 0 ) {
            logger.info("cloud-sync - no indices remaining to sync.");
            throw new IOException("No indices remaining to sync.");
        }

        IndexInfo first = filtered.get(0);
        //update the state to in-progress
        writeState(new IndexInfo(first.getName(),first.getSizeInBytes(), IndexInfo.State.SNAPSHOT_INPROGRESS), listSS);

        //take snapshot
        client.takeSnapshot(repository, snapshotNamePrefix +first.getName(),first.getName());

        //update state to snapshot done
        writeState(new IndexInfo(first.getName(),first.getSizeInBytes(), IndexInfo.State.SNAPSHOTED),listSS);
    }

    private void writeState(IndexInfo modified, List<IndexInfo> list) throws IOException {
        List<IndexInfo> listSS = Utils.replace(modified,list);
        client.writeState(Utils.toJson(listSS));
    }
}
