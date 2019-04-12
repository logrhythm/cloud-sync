package org.elasticsearch.sync.cloud.start;

import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Map;

/**
 * curl -X POST "localhost:9200/cloudsync/start" -H 'Content-Type: application/json' -d'
 * {
 *     "mode" : "source",
 *     "store" : "fs" / "aws-s3"/ "gcs"
 *     "indices" : "logs-*",
 *     "location": "/mount/backup", in case of s3 and gcs it bucket names.
 * }
 */

final public class StartInfo {
    public static final String SOURCE = "source";
    public static final String SINK = "sink";
    private String mode;
    private String indices;
    private String store;
    private String location;
    private String rawJson;

    public StartInfo(String json) throws IOException {
        this.rawJson = json;
        this.parse();
        this.validate();
    }

    private void validate() throws IOException {
        //Split logic based on source or sink
        if (!(StartInfo.SOURCE.equals(getMode()) || StartInfo.SINK.equals(getMode()))){
            throw new InputMismatchException("cloud-sync Invalid mode. Provide source or sink.");
        }
    }

    private void parse(){
        Map<String, Object> mapValue = XContentHelper.convertToMap(JsonXContent.jsonXContent, rawJson, false);
        this.mode = (String)mapValue.get("mode");
        this.indices = (String)mapValue.get("indices");
        this.store = (String)mapValue.get("store");
        this.location = (String)mapValue.get("location");
    }

    public String getMode() {
        return mode;
    }

    public String getIndices() {
        return indices;
    }

    public String getStore() {
        return store;
    }

    public String getLocation() {
        return location;
    }

    public String toJson() {
        return rawJson;
    }

    @Override
    public String toString() {
        return rawJson;
    }
}
