/*
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.analytics.datasource.rdbms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.analytics.dataservice.AnalyticsDSUtils;
import org.wso2.carbon.analytics.dataservice.AnalyticsDataService;
import org.wso2.carbon.analytics.dataservice.AnalyticsServiceHolder;
import org.wso2.carbon.analytics.dataservice.clustering.AnalyticsClusterException;
import org.wso2.carbon.analytics.dataservice.clustering.AnalyticsClusterManager;
import org.wso2.carbon.analytics.dataservice.clustering.GroupEventListener;
import org.wso2.carbon.analytics.dataservice.indexing.IndexType;
import org.wso2.carbon.analytics.dataservice.indexing.SearchResultEntry;
import org.wso2.carbon.analytics.datasource.core.AnalyticsRecordStoreTest;
import org.wso2.carbon.analytics.datasource.core.AnalyticsException;
import org.wso2.carbon.analytics.datasource.core.rs.Record;
import org.wso2.carbon.base.MultitenantConstants;

/**
 * This class represents the analytics data service tests.
 */
public class AnalyticsDataServiceTest implements GroupEventListener {

    protected AnalyticsDataService service;
    
    private boolean becameLeader, leaderUpdated;
               
    public void init(AnalyticsDataService service) {
        this.service = service;
    }
    
    @Test
    public void testIndexAddRetrieve() throws AnalyticsException {
        this.indexAddRetrieve(MultitenantConstants.SUPER_TENANT_ID);
        this.indexAddRetrieve(1);
        this.indexAddRetrieve(15001);
    }
    
    private void indexAddRetrieve(int tenantId) throws AnalyticsException {
        this.service.clearIndices(tenantId, "T1");
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("C1", IndexType.STRING);
        columns.put("C2", IndexType.STRING);
        columns.put("C3", IndexType.INTEGER);
        columns.put("C4", IndexType.LONG);
        columns.put("C5", IndexType.DOUBLE);
        columns.put("C6", IndexType.FLOAT);
        this.service.setIndices(tenantId, "T1", columns);
        Map<String, IndexType> columnsIn = service.getIndices(tenantId, "T1");
        Assert.assertEquals(columnsIn, columns);
        this.service.clearIndices(tenantId, "T1");
    }
    
    private void cleanupTable(int tenantId, String tableName) throws AnalyticsException {
        if (this.service.tableExists(tenantId, tableName)) {
            this.service.deleteTable(tenantId, tableName);
        }
        this.service.clearIndices(tenantId, tableName);
    }
    
    private List<Record> generateIndexRecords(int tenantId, String tableName, int n, long startTimestamp) {
        Map<String, Object> values = new HashMap<String, Object>();
        Record record;
        List<Record> result = new ArrayList<Record>();
        for (int i = 0; i < n; i++) {
            values = new HashMap<String, Object>();
            values.put("INT1", i);
            values.put("STR1", "STRING" + i);
            values.put("str2", "string" + i);
            values.put("TXT1", "My name is bill" + i);
            values.put("LN1", 1435000L + i);
            values.put("DB1", 54.535 + i);
            values.put("FL1", 3.14 + i);
            values.put("BL1", i % 2 == 0 ? true : false);
            record = new Record(tenantId, tableName, values, startTimestamp + i * 10);
            result.add(record);
        }        
        return result;
    }
    
    private void indexDataAddRetrieve(int tenantId, String tableName, int n) throws AnalyticsException {
        this.cleanupTable(tenantId, tableName);
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("INT1", IndexType.INTEGER);
        columns.put("STR1", IndexType.STRING);
        columns.put("str2", IndexType.STRING);
        columns.put("TXT1", IndexType.STRING);
        columns.put("LN1", IndexType.LONG);
        columns.put("DB1", IndexType.DOUBLE);
        columns.put("FL1", IndexType.FLOAT);
        columns.put("BL1", IndexType.BOOLEAN);
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        List<Record> records = this.generateIndexRecords(tenantId, tableName, n, 0);
        this.service.put(records);
        this.service.waitForIndexing(-1);
        List<SearchResultEntry> result = this.service.search(tenantId, tableName, "lucene", "STR1:STRING0", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "str2:string0", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "str2:String0", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "TXT1:name", 0, n + 10);
        Assert.assertEquals(result.size(), n);
        result = this.service.search(tenantId, tableName, "lucene", "INT1:" + (n - 1), 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "LN1:1435000", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "DB1:54.535", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "FL1:3.14", 0, 10);
        Assert.assertEquals(result.size(), 1);
        result = this.service.search(tenantId, tableName, "lucene", "BL1:true", 0, 10);
        Assert.assertTrue(result.size() > 0);
        if (n > 4) {
            result = this.service.search(tenantId, tableName, "lucene", "INT1:[1 TO 3]", 0, 10);
            Assert.assertEquals(result.size(), 3);
            result = this.service.search(tenantId, tableName, "lucene", "LN1:[1435000 TO 1435001]", 0, 10);
            Assert.assertEquals(result.size(), 2);
            result = this.service.search(tenantId, tableName, "lucene", "LN1:[1435000 TO 1435001}", 0, 10);
            Assert.assertEquals(result.size(), 1);
            result = this.service.search(tenantId, tableName, "lucene", "DB1:[54.01 TO 55.86]", 0, 10);
            Assert.assertEquals(result.size(), 2);
            result = this.service.search(tenantId, tableName, "lucene", "FL1:[3.01 TO 4.50]", 0, 10);
            Assert.assertEquals(result.size(), 2);
            result = this.service.search(tenantId, tableName, "lucene", "BL1:[false TO true]", 0, 10);
            Assert.assertTrue(result.size() > 2);
        }
        this.cleanupTable(tenantId, tableName);
    }
    
    @Test
    public void testIndexedDataAddRetrieve() throws AnalyticsException {
        this.indexDataAddRetrieve(5, "TX", 1);
        this.indexDataAddRetrieve(5, "TX", 10);
        this.indexDataAddRetrieve(6, "TX", 150);
        this.indexDataAddRetrieve(7, "TX", 2500);
    }
    
    @Test
    public void testSearchCount() throws AnalyticsException {
        int tenantId = 4;
        String tableName = "Books";
        this.cleanupTable(tenantId, tableName);
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("INT1", IndexType.INTEGER);
        columns.put("STR1", IndexType.STRING);
        columns.put("str2", IndexType.STRING);
        columns.put("TXT1", IndexType.STRING);
        columns.put("LN1", IndexType.LONG);
        columns.put("DB1", IndexType.DOUBLE);
        columns.put("FL1", IndexType.FLOAT);
        columns.put("BL1", IndexType.BOOLEAN);
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        List<Record> records = this.generateIndexRecords(tenantId, tableName, 100, 0);
        this.service.put(records);
        this.service.waitForIndexing(-1);
        int count = this.service.searchCount(tenantId, tableName, "lucene", "STR1:STRING55");
        Assert.assertEquals(count, 1);
        count = this.service.searchCount(tenantId, tableName, "lucene", "TXT1:name");
        Assert.assertEquals(count, 100);
        this.cleanupTable(tenantId, tableName);
    }
    
    @Test
    public void testIndexedDataUpdate() throws Exception {
        int tenantId = 1;
        String tableName = "T1";
        this.cleanupTable(tenantId, tableName);
        this.service.createTable(tenantId, tableName);
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("STR1", IndexType.STRING);
        columns.put("STR2", IndexType.STRING);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("STR1", "Sri Lanka is known for tea");
        values.put("STR2", "Cricket is most famous");
        Map<String, Object> values2 = new HashMap<String, Object>();
        values2.put("STR1", "Canada is known for Ice Hockey");
        values2.put("STR2", "It is very cold");
        Record record = new Record(tenantId, tableName, values, System.currentTimeMillis());
        Record record2 = new Record(tenantId, tableName, values2, System.currentTimeMillis());
        List<Record> records = new ArrayList<Record>();
        records.add(record);
        records.add(record2);
        this.service.setIndices(tenantId, tableName, columns);
        this.service.put(records);
        this.service.waitForIndexing(-1);
        List<SearchResultEntry> result = this.service.search(tenantId, tableName, "lucene", "STR1:tea", 0, 10);
        Assert.assertEquals(result.size(), 1);
        String id = record.getId();
        Assert.assertEquals(result.get(0).getId(), id);
        result = this.service.search(tenantId, tableName, "lucene", "STR1:diamonds", 0, 10);
        Assert.assertEquals(result.size(), 0);
        result = this.service.search(tenantId, tableName, "lucene", "STR2:cricket", 0, 10);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getId(), id);
        values = new HashMap<String, Object>();
        values.put("STR1", "South Africa is know for diamonds");
        values.put("STR2", "NBA has the best basketball action");
        record = new Record(id, tenantId, tableName, values, System.currentTimeMillis());
        records = new ArrayList<Record>();
        records.add(record);
        /* update */
        this.service.put(records);
        this.service.waitForIndexing(-1);
        result = this.service.search(tenantId, tableName, "lucene", "STR1:tea", 0, 10);
        Assert.assertEquals(result.size(), 0);
        result = this.service.search(tenantId, tableName, "lucene", "STR2:cricket", 0, 10);
        Assert.assertEquals(result.size(), 0);
        result = this.service.search(tenantId, tableName, "lucene", "STR1:diamonds", 0, 10);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getId(), id);
        result = this.service.search(tenantId, tableName, "lucene", "STR2:basketball", 0, 10);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getId(), id);
        result = this.service.search(tenantId, tableName, "lucene", "STR1:hockey", 0, 10);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getId(), record2.getId());
        this.cleanupTable(tenantId, tableName);
    }
    
    @Test
    public void testIndexDataDeleteWithIds() throws AnalyticsException {
        int tenantId = 5100;
        String tableName = "X1";
        this.cleanupTable(tenantId, tableName);
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("INT1", IndexType.INTEGER);
        columns.put("STR1", IndexType.STRING);
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        List<Record> records = this.generateIndexRecords(tenantId, tableName, 98, 0);
        this.service.put(records);
        List<String> ids = new ArrayList<String>();
        ids.add(records.get(0).getId());
        ids.add(records.get(5).getId());
        ids.add(records.get(50).getId());
        ids.add(records.get(97).getId());
        Assert.assertEquals(AnalyticsDSUtils.listRecords(this.service, this.service.get(tenantId, tableName, null, ids)).size(), 4);
        this.service.waitForIndexing(-1);
        List<SearchResultEntry> result = this.service.search(tenantId, tableName, "lucene", "STR1:S*", 0, 150);
        Assert.assertEquals(result.size(), 98);
        this.service.delete(tenantId, tableName, ids);
        this.service.waitForIndexing(-1);
        result = this.service.search(tenantId, tableName, "lucene", "STR1:S*", 0, 150);
        Assert.assertEquals(result.size(), 94);
        Assert.assertEquals(AnalyticsDSUtils.listRecords(this.service, this.service.get(tenantId, tableName, null, ids)).size(), 0);
        this.cleanupTable(tenantId, tableName);
    }
    
    @Test
    public void testIndexDataDeleteRange() throws AnalyticsException {
        int tenantId = 230;
        String tableName = "Scores";
        int n = 115;
        this.cleanupTable(tenantId, tableName);
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("INT1", IndexType.INTEGER);
        columns.put("STR1", IndexType.STRING);
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        List<Record> records = this.generateIndexRecords(tenantId, tableName, n, 1000);
        this.service.put(records);
        List<Record> recordsIn = AnalyticsDSUtils.listRecords(this.service, 
                this.service.get(tenantId, tableName, null, -1, -1, 0, -1));
        Assert.assertEquals(recordsIn.size(), n);
        this.service.delete(tenantId, tableName, 1030, 1060);
        recordsIn = AnalyticsDSUtils.listRecords(this.service, 
                this.service.get(tenantId, tableName, null, -1, -1, 0, -1));
        Assert.assertEquals(recordsIn.size(), n - 3);
        /* lets test table name case-insensitiveness too */
        this.service.waitForIndexing(-1);
        List<SearchResultEntry> results = this.service.search(tenantId, 
                tableName.toUpperCase(), "lucene", "STR1:s*", 0, n);
        Assert.assertEquals(results.size(), n - 3);
        this.cleanupTable(tenantId, tableName);
    }
    
    //@Test
    public void testDataRecordAddReadPerformanceNonIndex() throws AnalyticsException {
        this.cleanupTable(50, "TableX");
        System.out.println("\n************** START ANALYTICS DS (WITHOUT INDEXING, H2-FILE) PERF TEST **************");
        int n = 100, batch = 200;
        List<Record> records;
        
        /* warm-up */
        this.service.createTable(50, "TableX");      
        for (int i = 0; i < 10; i++) {
            records = AnalyticsRecordStoreTest.generateRecords(50, "TableX", i, batch, -1, -1);
            this.service.put(records);
        }
        this.cleanupTable(50, "TableX");
        
        this.service.createTable(50, "TableX");
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            records = AnalyticsRecordStoreTest.generateRecords(50, "TableX", i, batch, -1, -1);
            this.service.put(records);
        }
        long end = System.currentTimeMillis();
        System.out.println("* Records: " + (n * batch));
        System.out.println("* Write Time: " + (end - start) + " ms.");
        System.out.println("* Write Throughput (TPS): " + (n * batch) / (double) (end - start) * 1000.0);
        List<Record> recordsIn = AnalyticsDSUtils.listRecords(this.service, this.service.get(50, "TableX", null, -1, -1, 0, -1));
        Assert.assertEquals(recordsIn.size(), (n * batch));
        end = System.currentTimeMillis();
        System.out.println("* Read Time: " + (end - start) + " ms.");
        System.out.println("* Read Throughput (TPS): " + (n * batch) / (double) (end - start) * 1000.0);
        this.cleanupTable(50, "TableX");
        System.out.println("\n************** END ANALYTICS DS (WITHOUT INDEXING, H2-FILE) PERF TEST **************");
    }
    
    private void writeIndexRecords(int tenantId, String tableName, int n, int batch) throws AnalyticsException {
        List<Record> records;
        for (int i = 0; i < n; i++) {
            records = AnalyticsRecordStoreTest.generateRecords(tenantId, tableName, i, batch, -1, -1);
            this.service.put(records);
        }
    }
    
    @Test
    public void testDataRecordAddReadPerformanceIndex1C() throws AnalyticsException {
        System.out.println("\n************** START ANALYTICS DS (WITH INDEXING - SINGLE THREAD, H2-FILE) PERF TEST **************");

        int tenantId = 50;
        String tableName = "TableX";
        this.cleanupTable(tenantId, tableName);
        int n = 250, batch = 200;
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("tenant", IndexType.INTEGER);
        columns.put("ip", IndexType.STRING);
        columns.put("log", IndexType.STRING);        
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        
        long start = System.currentTimeMillis();
        this.writeIndexRecords(tenantId, tableName, n, batch);
        this.service.waitForIndexing(-1);
        long end = System.currentTimeMillis();
        System.out.println("* Records: " + (n * batch));
        System.out.println("* Write Time: " + (end - start) + " ms.");
        System.out.println("* Write Throughput (TPS): " + (n * batch) / (double) (end - start) * 1000.0);
        start = System.currentTimeMillis();
        List<Record> recordsIn = AnalyticsDSUtils.listRecords(this.service, this.service.get(tenantId, tableName, null, -1, -1, 0, -1));
        Assert.assertEquals(recordsIn.size(), (n * batch));
        end = System.currentTimeMillis();
        System.out.println("* Read Time: " + (end - start) + " ms.");
        System.out.println("* Read Throughput (TPS): " + (n * batch) / (double) (end - start) * 1000.0);        
        start = System.currentTimeMillis();
        List<SearchResultEntry> results = this.service.search(tenantId, tableName, "lucene", "log: exception", 0, 75);
        end = System.currentTimeMillis();
        Assert.assertEquals(results.size(), 75);
        System.out.println("* Search Result Count: " + results.size() + " Time: " + (end - start) + " ms.");
        
        this.cleanupTable(tenantId, tableName);
        System.out.println("\n************** END ANALYTICS DS (WITH INDEXING, H2-FILE) PERF TEST **************");
    }
    
    private void writeIndexRecords(final int tenantId, final String tableName, final int n, 
            final int batch, final int nThreads) throws AnalyticsException {
        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            es.execute(new Runnable() {            
                @Override
                public void run() {
                    try {
                        writeIndexRecords(tenantId, tableName, n, batch);
                    } catch (AnalyticsException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        try {
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new AnalyticsException(e.getMessage(), e);
        }
    }
    
    @Test
    public void testDataRecordAddReadPerformanceIndexNC() throws AnalyticsException {
        System.out.println("\n************** START ANALYTICS DS (WITH INDEXING - MULTIPLE THREADS, H2-FILE) PERF TEST **************");

        int tenantId = 50;
        String tableName = "TableX";
        this.cleanupTable(tenantId, tableName);        
        int n = 50, batch = 200, nThreads = 5;
        Map<String, IndexType> columns = new HashMap<String, IndexType>();
        columns.put("tenant", IndexType.INTEGER);
        columns.put("ip", IndexType.STRING);
        columns.put("log", IndexType.STRING);        
        this.service.createTable(tenantId, tableName);
        this.service.setIndices(tenantId, tableName, columns);
        
        long start = System.currentTimeMillis();
        this.writeIndexRecords(tenantId, tableName, n, batch, nThreads);
        this.service.waitForIndexing(-1);
        long end = System.currentTimeMillis();
        System.out.println("* Records: " + (n * batch * nThreads));
        System.out.println("* Write Time: " + (end - start) + " ms.");
        System.out.println("* Write Throughput (TPS): " + (n * batch * nThreads) / (double) (end - start) * 1000.0);
        start = System.currentTimeMillis();
        List<Record> recordsIn = AnalyticsDSUtils.listRecords(this.service, this.service.get(tenantId, tableName, null, -1, -1, 0, -1));
        Assert.assertEquals(recordsIn.size(), (n * batch * nThreads));
        end = System.currentTimeMillis();
        System.out.println("* Read Time: " + (end - start) + " ms.");
        System.out.println("* Read Throughput (TPS): " + (n * batch * nThreads) / (double) (end - start) * 1000.0);        
        start = System.currentTimeMillis();
        List<SearchResultEntry> results = this.service.search(tenantId, tableName, "lucene", "log: exception", 0, 75);
        end = System.currentTimeMillis();
        Assert.assertEquals(results.size(), 75);
        System.out.println("* Search Result Count: " + results.size() + " Time: " + (end - start) + " ms.");
        
        this.cleanupTable(tenantId, tableName);
        System.out.println("\n************** END ANALYTICS DS (WITH INDEXING - MULTIPLE THREADS, H2-FILE) PERF TEST **************");
    }
    
    private void resetClusterTestResults() {
        this.becameLeader = false;
        this.leaderUpdated = false;
    }
    
    @Test
    public void testAnalyticsClusterManager() throws AnalyticsClusterException {
        AnalyticsClusterManager acm = AnalyticsServiceHolder.getAnalyticsClusterManager();
        if (!acm.isClusteringEnabled()) {
            return;
        }
        this.resetClusterTestResults();        
        acm.joinGroup("G1", this);
        Assert.assertTrue(this.leaderUpdated);
        int value = 5;
        List<Integer> result = acm.executeAll("G1", new ClusterGroupTestMessage(value));
        Assert.assertTrue(result.size() > 0);
        Assert.assertEquals((int) result.get(0), value + 1);
        List<Object> members = acm.getMembers("G1");
        Assert.assertTrue(members.size() > 0);
        int result2 = acm.executeOne("G1", members.get(0), new ClusterGroupTestMessage(value + 1));
        Assert.assertEquals(result2, value + 2);
        this.resetClusterTestResults();
    }

    @Test (enabled = false)
    @Override
    public void onBecomingLeader() {
        this.becameLeader = true;
        AnalyticsServiceHolder.getAnalyticsClusterManager().setProperty("G1", "location", "10.0.0.2");
    }

    @Test (enabled = false)
    @Override
    public void onLeaderUpdate() {
        this.leaderUpdated = true;
        Assert.assertTrue(this.becameLeader);
        String location = (String) AnalyticsServiceHolder.getAnalyticsClusterManager().getProperty("G1", "location");
        Assert.assertEquals(location, "10.0.0.2");
        location = (String) AnalyticsServiceHolder.getAnalyticsClusterManager().getProperty("GX", "location");
        Assert.assertNull(location);
    }

    @Override
    public void onMembersChangeForLeader() {
        /* nothing to do */
    }
    
    /**
     * Test cluster message implementation.
     */
    public static class ClusterGroupTestMessage implements Callable<Integer>, Serializable {

        private static final long serialVersionUID = 3918252455368655212L;
        
        private int data;
                
        public ClusterGroupTestMessage(int data) {
            this.data = data;
        }
        
        @Override
        public Integer call() throws Exception {
            return data + 1;
        }
        
    }

}
