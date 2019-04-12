package org.elasticsearch.sync.cloud.utils;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Utils {

    private Utils(){
        //empty
    }


    public static String toJson(Map<String, Long> indices) throws IOException {

        XContentBuilder builder = jsonBuilder().startObject()
                .field("name","cloudsync")
                .field("snapshots")
                .startArray();

        for(String index : indices.keySet()){
            IndexInfo ss = new IndexInfo(index, indices.get(index), IndexInfo.State.READY);
            ss.toXContent(builder);
        }
        builder.endArray();
        builder.endObject();
        return builder.string();
    }

    public static String toJson(List<IndexInfo> ss) throws IOException {

        XContentBuilder builder = jsonBuilder().startObject()
                .field("name","cloudsync")
                .field("snapshots")
                .startArray();

        for(IndexInfo s : ss){
            s.toXContent(builder);
        }
        builder.endArray();
        builder.endObject();
        return builder.string();
    }

    public static List<IndexInfo> toSnapshots(String json){

        if(json == "" || json.isEmpty())
            return new ArrayList<>();

        Map<String, Object> mapValue = XContentHelper.convertToMap(JsonXContent.jsonXContent, json, false);
        ArrayList<Object> objList = (ArrayList<Object>) mapValue.get("snapshots");
        List<IndexInfo> ssList = new ArrayList<>();
        for(Object obj : objList ){
            IndexInfo ss = new IndexInfo((Map<String,Object>)obj);
            ssList.add(ss);
        }
        return ssList;
    }

    public static List<IndexInfo> sortAndFilter(List<IndexInfo> ss, IndexInfo.State filteredState){

        List<IndexInfo> filtered = new ArrayList<>();
        for(IndexInfo s : ss){
            if(filteredState == s.getState()){
                filtered.add(s);
            }
        }
        Collections.sort(filtered);
        return filtered;
    }

    public static List<IndexInfo> replace(IndexInfo indexInfo, List<IndexInfo> ss){

        int i = 0;
        for(IndexInfo s : ss){
            if(s.getName().equals(indexInfo.getName())){
                break;
            }
            i++;
        }
        ss.remove(i);
        ss.add(0, indexInfo);
        return ss;
    }
}
