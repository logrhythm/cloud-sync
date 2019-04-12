package org.elasticsearch.sync.cloud.utils;



import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class IndexInfoTest {

    @Test
    public void testIndexInfoWithJson() {
        String json1 = "{\n" +
                "      \"name\": \"logs-2019-01-01\",\n" +
                "      \"sizeInBytes\": 21345667,\n" +
                "      \"state\": \"READY\"\n" +
                "    }";
        IndexInfo indx1 = new IndexInfo(json1);

        assertEquals("logs-2019-01-01",indx1.getName());
        assertEquals(21345667L,indx1.getSizeInBytes().longValue());
        assertEquals("READY",indx1.getState().toString());
    }

    @Test
    public void testIndexInfoWithMap() {
        Map<String,Object> map = new HashMap<>();
        map.put("name","logs-2019-01-01");
        map.put("sizeInBytes",21345667);
        map.put("state","READY");

        IndexInfo indx1 = new IndexInfo(map);
        assertEquals("logs-2019-01-01",indx1.getName());
        assertEquals(21345667L,indx1.getSizeInBytes().longValue());
        assertEquals("READY",indx1.getState().toString());
    }

    @Test
    public void testSnapshotCreate() throws IOException  {
        IndexInfo indexInfo = new IndexInfo("logs-2019-01-01",Long.valueOf(21345667), IndexInfo.State.SNAPSHOTED);
        assertEquals("logs-2019-01-01", indexInfo.getName());
        assertEquals(21345667L, indexInfo.getSizeInBytes().longValue());
        assertEquals("SNAPSHOTED", indexInfo.getState().toString());

        System.out.println("JSON packet: "+ indexInfo.toJson());

        IndexInfo ss = new IndexInfo(indexInfo.toJson());
        assertEquals("logs-2019-01-01",ss.getName());
        assertEquals(21345667L,ss.getSizeInBytes().longValue());
        assertEquals("SNAPSHOTED",ss.getState().toString());
    }

}
