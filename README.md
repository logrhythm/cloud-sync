### cloud-sync
* Runs as Elasticsearch plugin.    
* CloudSync uses Elasticsearch Snapshot and Restore API's and provides a "streaming service" to move data 
from one cluster to another cluster. 
 

##### Use Case(s)
1. Provides a simple, cost effective way of moving existing Elasticsearch indices from on-prem cluster to Cloud.
1. Curerntly this is developed and test with GCP. AWS support is more of test effort.
1. This supports moving 100's of terabytes of Elasticsearch indices.     
1. Support blue/green deployment process. We want to do this with (almost) zero downtime.
1. Provides visibility on the sync progress.

##### CloudSync Install
1. Install cloudsync
    1. On both source and sink clusters.   
    `
    /usr/share/elasticsearch/bin/elasticsearch-plugin install file:///home/logrhythm/cloud-sync-1.0.0-SNAPSHOT.zip
    `   
1. Install GCS plugin
    1. Download GCS plugin from: https://artifacts.elastic.co/downloads/elasticsearch-plugins/repository-gcs/repository-gcs-5.6.3.zip
    1. On both source and sink clusters.  
    `
    bin/elasticsearch-plugin install file:///home/logrhythm/hack/repository-gcs-5.6.3.zip
    `
    1. Follow steps from: https://www.elastic.co/guide/en/elasticsearch/plugins/5.6/repository-gcs-usage.html 
    1. Adding GCS creds to the Elasticsearch keystore. (On Source)
     ` 
     /usr/share/elasticsearch/bin/elasticsearch-keystore add-file gcs.client.default.credentials_file /gcs/my_file.json
     `
    1. If Sink cluster is on GCP, it doesnt needs GCS creds in Elasticsearch keystore.
        

##### CloudSync API
1. Start the Source:  
    `
    PUT /cloudsync/start
    {
        "mode" : "source",  
        "store" : "fs",             
        "indices" : "logs-*",
        "location": "/mount/cloudsync_backup",
    }
    ` 
    1. Other stores supported: aws-s3, gcp..
    1. This needs to be issued with every restart.
    
1. Start the Sink:
    `
    PUT /cloudsync/start
    {
        "mode" : "sink",
        "store" : "fs",
        "location": "/mount/cloudsync_backup",
    }
    `

1. Curl examples for 'filesystem' nfs store. 

    1. Start Source
    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "source",
      "store": "fs",  
      "indices": "logs-*",
      "location": "/opt/lr/cloudsync"
    }'
    `    
    1. Start Sink
    
    `
    curl -X POST "localhost:9201/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
        "mode": "sink",
        "store": "fs",  
        "indices": "logs-*",
        "location": "/opt/lr/cloudsync"
    }'
    `
1. Curl examples for 'gcs' store 

    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "source",
      "store": "gcs",  
      "indices": "logs-*",
      "location": "dx_cloud"
    }'
    `
    `
    curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
    {
      "mode": "sink",
      "store": "gcs",  
      "indices": "logs-*",
      "location": "dx_cloud"
    }'
    `


1. Sync Status on source cluster 
   
    `GET /cloudsync/status`

    `curl localhost:9200/cloudsync/status`

##### Development

1. Built using maven.
1. Elasticsearch plugin need to have exact version as Elasticsearch. To build for different versions change pom.xml
    `<properties>
        <elasticsearch.version>5.6.3</elasticsearch.version>
    </properties>
    `
1. cloud-sync-<version>.zip is found in `target/releases/` after `mvn clean install`

##### Backlog
1. Fix response header type missing.     
1. Tested only when this plugin is installed to singe node in cluster. Support multiple nodes for high availability.


#### FAQS

1. "failed":"no such index"  => your indices patterns resolved to zero indices on source cluster.

#### Quick Reference: Elasticsearch Snapshot & Restore API

1. Verify repository is present
    `
    curl -X POST "localhost:9200/_snapshot/cloudsync_backup/_verify"     
    `
1. Create repository
    `
    curl -XPUT 'http://localhost:9200/_snapshot/cloudsync_backup' -H 'Content-Type: application/json' -d '{
        "type": "fs",
        "settings": {
            "location": "/opt/lr/cloudsync",
            "compress": true
        }
    }'
    `
    
1. Delete repository 
  1. All snapshots must be deleted before this operation)
    `
    curl -X DELETE "localhost:9200/_snapshot/cloudsync_backup"
    `   

1. Snapshot an index:
    `
    curl -X PUT "localhost:9200/_snapshot/cloudsync_backup/snapshot_1" -H 'Content-Type: application/json' -d'
    {
    
      "indices": "logs-2017-10-21",
      "ignore_unavailable": true,
      "include_global_state": false,
      "chunk_size": "10m"
    }'
    `

2. Check indexInfo status: 
    `curl -X GET "localhost:9200/_snapshot/cloudsync_backup/snapshot_1/_status?pretty"`

3. Delete a indexInfo: 
    `curl -X DELETE "localhost:9200/_snapshot/cloudsync_backup/snapshot_1"`

4. Restore a indexInfo: 
    `curl -X POST "localhost:9201/_snapshot/cloudsync_backup/snapshot_1/_restore"`

5. List of all utils  
    `curl -X GET "localhost:9200/_snapshot/cloudsync_backup/_all?pretty"`

