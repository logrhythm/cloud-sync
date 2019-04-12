package org.elasticsearch.sync.cloud.utils;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class UtilsTest  {

    @Test
    public void testToJsonfromMap() throws IOException, JSONException {
        Map<String,Long> indices = new HashMap<>();
        //test empty input.
        String json1 = Utils.toJson(indices);
        String expected1 = "{\"name\":\"cloudsync\",\"snapshots\":[]}";
        JSONAssert.assertEquals(expected1,json1,false);

        //test with valid indices.
        indices.put("logs-2019-01-01",34L);
        indices.put("logs-2019-01-02",234L);
        indices.put("logs-2019-01-03",1234L);

        String json2 = Utils.toJson(indices);
        String expected2 = "{\"name\":\"cloudsync\",\"snapshots\":[{\"name\":\"logs-2019-01-03\",\"sizeInBytes\":1234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-02\",\"sizeInBytes\":234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-01\",\"sizeInBytes\":34,\"state\":\"READY\"}]}";
        JSONAssert.assertEquals(expected2,json2,false);
    }

    @Test
    public void testToJsonFromList() throws IOException, JSONException {
        List<IndexInfo> ss = new ArrayList<>();
        String json1 = Utils.toJson(ss);
        String expected1 = "{\"name\":\"cloudsync\",\"snapshots\":[]}";
        JSONAssert.assertEquals(expected1,json1,false);


        ss.add( new IndexInfo("logs-2019-01-01",34L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-02",234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-03",1234L, IndexInfo.State.READY));

        String json2 = Utils.toJson(ss);
        String expected2 = "{\"name\":\"cloudsync\",\"snapshots\":[{\"name\":\"logs-2019-01-03\",\"sizeInBytes\":1234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-02\",\"sizeInBytes\":234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-01\",\"sizeInBytes\":34,\"state\":\"READY\"}]}";
        JSONAssert.assertEquals(expected2,json2,false);
    }

    @Test
    public void testSnapshots() throws IOException {
        String json1 = "";
        List<IndexInfo> list1 = Utils.toSnapshots(json1);
        assertNotEquals(null,list1);
        assertEquals(0,list1.size());

        String json2 = "{\"name\":\"cloudsync\",\"snapshots\":[{\"name\":\"logs-2019-01-03\",\"sizeInBytes\":1234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-02\",\"sizeInBytes\":234,\"state\":\"READY\"},{\"name\":\"logs-2019-01-01\",\"sizeInBytes\":34,\"state\":\"READY\"}]}";
        List<IndexInfo> list2 = Utils.toSnapshots(json2);
        assertNotEquals(null,list2);
        assertEquals(3,list2.size());
    }

    @Test
    public void testSnapshotsSortFilter() throws IOException {
        Map<String,Long> indices = new HashMap<>();
        indices.put("logs-2019-01-01",1234L);
        indices.put("logs-2019-01-02",234L);
        indices.put("logs-2019-01-03",34L);

        String json = Utils.toJson(indices);
        List<IndexInfo> ss = Utils.toSnapshots(json);
        List<IndexInfo> filtered = Utils.sortAndFilter(ss, IndexInfo.State.READY);

        assertEquals(3,filtered.size());

        assertEquals("logs-2019-01-03",filtered.get(0).getName());
        assertEquals("logs-2019-01-01",filtered.get(2).getName());
    }

    @Test
    public void testReplace() throws IOException {
        List<IndexInfo> ss = new ArrayList<>();

        ss.add( new IndexInfo("logs-2019-01-01",1234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-02",234L, IndexInfo.State.READY));
        ss.add( new IndexInfo("logs-2019-01-03",34L, IndexInfo.State.READY));

        assertEquals(IndexInfo.State.READY,ss.get(0).getState());
        ss = Utils.replace(new IndexInfo("logs-2019-01-03",34L, IndexInfo.State.SNAPSHOTED),ss);

        assertEquals(3,ss.size());
        assertEquals(IndexInfo.State.SNAPSHOTED,ss.get(0).getState());
    }
}
