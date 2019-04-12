package org.elasticsearch.sync.cloud.utils;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

final public class IndexInfo implements Comparable {

    public enum State {
        READY,
        SNAPSHOT_INPROGRESS,
        SNAPSHOTED;

        static State toState(String s){
            switch (s){
                case "READY":
                    return READY;
                case "SNAPSHOT_INPROGRESS":
                    return SNAPSHOT_INPROGRESS;
                case "SNAPSHOTED":
                    return SNAPSHOTED;
                default:
                    return null;
            }
        }
    };

    private String name;
    private Long sizeInBytes;
    private State state;

    public IndexInfo(String name, Long sizeInBytes, State state){
        this.name = name;
        this.sizeInBytes = sizeInBytes;
        this.state = state;
    }

    public IndexInfo(String json){
        Map<String, Object> mapValue = XContentHelper.convertToMap(JsonXContent.jsonXContent, json, false);
        init(mapValue);
    }

    public IndexInfo(Map<String,Object> mapValue) {
        init(mapValue);
    }

    private void init(Map<String,Object> mapValue) {
        this.name = (String)mapValue.get("name");
        this.sizeInBytes = new Long((Integer)mapValue.get("sizeInBytes"));
        this.state = State.toState((String)mapValue.get("state"));
    }

    public State getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (IOException ex){
            return null;
        }
    }

    public XContentBuilder toXContent(XContentBuilder builder) throws IOException{
        return builder.startObject()
                .field("name",name)
                .field("sizeInBytes", sizeInBytes)
                .field("state",state)
                .endObject();

    }

    public String toJson() throws IOException {
        return toXContent(jsonBuilder()).string();
    }

    @Override
    public int compareTo(Object o) {
        IndexInfo that = (IndexInfo)o;
        //we want in desc.
        return that.name.compareTo(this.name);

    }
}
