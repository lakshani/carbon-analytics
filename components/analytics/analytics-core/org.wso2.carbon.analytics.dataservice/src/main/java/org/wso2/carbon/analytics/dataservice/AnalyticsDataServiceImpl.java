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
package org.wso2.carbon.analytics.dataservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.wso2.carbon.analytics.dataservice.config.AnalyticsDataServiceConfigProperty;
import org.wso2.carbon.analytics.dataservice.config.AnalyticsDataServiceConfiguration;
import org.wso2.carbon.analytics.dataservice.indexing.AnalyticsDataIndexer;
import org.wso2.carbon.analytics.dataservice.indexing.IndexType;
import org.wso2.carbon.analytics.dataservice.indexing.SearchResultEntry;
import org.wso2.carbon.analytics.datasource.core.AnalyticsException;
import org.wso2.carbon.analytics.datasource.core.AnalyticsTimeoutException;
import org.wso2.carbon.analytics.datasource.core.fs.AnalyticsFileSystem;
import org.wso2.carbon.analytics.datasource.core.rs.AnalyticsRecordStore;
import org.wso2.carbon.analytics.datasource.core.rs.AnalyticsTableNotAvailableException;
import org.wso2.carbon.analytics.datasource.core.rs.Record;
import org.wso2.carbon.analytics.datasource.core.rs.RecordGroup;

/**
 * The implementation of {@link AnalyticsDataService}.
 */
public class AnalyticsDataServiceImpl implements AnalyticsDataService {

    private AnalyticsRecordStore analyticsRecordStore;
        
    private AnalyticsDataIndexer indexer;
        
    public AnalyticsDataServiceImpl(AnalyticsRecordStore analyticsRecordStore,
            AnalyticsFileSystem analyticsFileSystem, int shardCount) throws AnalyticsException {
        this.analyticsRecordStore = analyticsRecordStore;
        this.indexer = new AnalyticsDataIndexer(analyticsRecordStore, analyticsFileSystem, shardCount);
        AnalyticsServiceHolder.setAnalyticsDataService(this);
        this.indexer.init();
    }
    
    public AnalyticsDataServiceImpl(AnalyticsDataServiceConfiguration config) throws AnalyticsException {
        AnalyticsRecordStore ars;
        AnalyticsFileSystem afs;
        Analyzer luceneAnalyzer;
        try {
            String arsClass = config.getAnalyticsRecordStoreConfiguration().getImplementation();
            String afsClass = config.getAnalyticsFileSystemConfiguration().getImplementation();
            String analyzerClass = config.getLuceneAnalyzerConfiguration().getImplementation();
            ars = (AnalyticsRecordStore) Class.forName(arsClass).newInstance();
            afs = (AnalyticsFileSystem) Class.forName(afsClass).newInstance();
            ars.init(this.convertToMap(config.getAnalyticsRecordStoreConfiguration().getProperties()));
            afs.init(this.convertToMap(config.getAnalyticsFileSystemConfiguration().getProperties()));
            luceneAnalyzer = (Analyzer) Class.forName(analyzerClass).newInstance();
        } catch (Exception e) {
            throw new AnalyticsException("Error in creating analytics data service from configuration: " + 
                    e.getMessage(), e);
        }
        this.analyticsRecordStore = ars;
        this.indexer = new AnalyticsDataIndexer(ars, afs, config.getShardCount(), luceneAnalyzer);
        AnalyticsServiceHolder.setAnalyticsDataService(this);
        this.indexer.init();
    }
    
    private Map<String, String> convertToMap(AnalyticsDataServiceConfigProperty[] props) {
        Map<String, String> result = new HashMap<String, String>();
        for (AnalyticsDataServiceConfigProperty prop : props) {
            result.put(prop.getName(), prop.getValue());
        }
        return result;
    }
    
    public AnalyticsDataIndexer getIndexer() {
        return indexer;
    }
    
    public AnalyticsRecordStore getAnalyticsRecordStore() {
        return analyticsRecordStore;
    }
    
    @Override
    public void createTable(int tenantId, String tableName) throws AnalyticsException {
        this.getAnalyticsRecordStore().createTable(tenantId, tableName);
    }

    @Override
    public boolean tableExists(int tenantId, String tableName) throws AnalyticsException {
        return this.getAnalyticsRecordStore().tableExists(tenantId, tableName);
    }

    @Override
    public void deleteTable(int tenantId, String tableName) throws AnalyticsException {
        this.getAnalyticsRecordStore().deleteTable(tenantId, tableName);
        this.clearIndices(tenantId, tableName);
    }

    @Override
    public List<String> listTables(int tenantId) throws AnalyticsException {
        return this.getAnalyticsRecordStore().listTables(tenantId);
    }

    @Override
    public long getRecordCount(int tenantId, 
            String tableName) throws AnalyticsException, AnalyticsTableNotAvailableException {
        return this.getAnalyticsRecordStore().getRecordCount(tenantId, tableName);
    }

    @Override
    public void put(List<Record> records) throws AnalyticsException, AnalyticsTableNotAvailableException {
        this.getAnalyticsRecordStore().put(records);
        this.getIndexer().insert(records);
    }
    
    @Override
    public RecordGroup[] get(int tenantId, String tableName, List<String> columns, long timeFrom, long timeTo,
            int recordsFrom, int recordsCount) throws AnalyticsException, AnalyticsTableNotAvailableException {
        return this.getAnalyticsRecordStore().get(tenantId, tableName, columns, timeFrom, 
                timeTo, recordsFrom, recordsCount);
    }

    @Override
    public RecordGroup[] get(int tenantId, String tableName, List<String> columns, List<String> ids)
            throws AnalyticsException, AnalyticsTableNotAvailableException {
        return this.getAnalyticsRecordStore().get(tenantId, tableName, columns, ids);
    }
    
    @Override
    public Iterator<Record> readRecords(RecordGroup recordGroup) throws AnalyticsException {
        return this.getAnalyticsRecordStore().readRecords(recordGroup);
    }

    @Override
    public void delete(int tenantId, String tableName, long timeFrom, long timeTo) throws AnalyticsException,
            AnalyticsTableNotAvailableException {
        this.getIndexer().delete(tenantId, tableName, 
                this.getRecordIdsFromTimeRange(tenantId, tableName, timeFrom, timeTo));
        this.getAnalyticsRecordStore().delete(tenantId, tableName, timeFrom, timeTo);
    }
    
    private List<String> getRecordIdsFromTimeRange(int tenantId, String tableName, long timeFrom, 
            long timeTo) throws AnalyticsException {
        List<Record> records = AnalyticsDSUtils.listRecords(this, 
                this.get(tenantId, tableName, null, timeFrom, timeTo, 0, -1));
        List<String> result = new ArrayList<>(records.size());
        for (Record record : records) {
            result.add(record.getId());
        }
        return result;
    }

    @Override
    public void delete(int tenantId, String tableName, List<String> ids) throws AnalyticsException,
            AnalyticsTableNotAvailableException {
        this.getIndexer().delete(tenantId, tableName, ids);
        this.getAnalyticsRecordStore().delete(tenantId, tableName, ids);
    }

    @Override
    public void setIndices(int tenantId, String tableName, 
            Map<String, IndexType> columns) throws AnalyticsIndexException {
        this.getIndexer().setIndices(tenantId, tableName, columns);
    }

    @Override
    public List<SearchResultEntry> search(int tenantId, String tableName, String language, String query,
            int start, int count) throws AnalyticsIndexException, AnalyticsException {
        return this.getIndexer().search(tenantId, tableName, language, query, start, count);
    }
    
    @Override
    public int searchCount(int tenantId, String tableName, String language, 
            String query) throws AnalyticsIndexException {
        return this.getIndexer().searchCount(tenantId, tableName, language, query);
    }

    @Override
    public Map<String, IndexType> getIndices(int tenantId, 
            String tableName) throws AnalyticsIndexException, AnalyticsException {
        return this.getIndexer().lookupIndices(tenantId, tableName);
    }

    @Override
    public void clearIndices(int tenantId, String tableName) throws AnalyticsIndexException, AnalyticsException {
        this.getIndexer().clearIndices(tenantId, tableName);
    }

    @Override
    public void waitForIndexing(long maxWait) throws AnalyticsException,
            AnalyticsTimeoutException {
        this.getIndexer().waitForIndexing(maxWait);
    }
    
    @Override
    public void destroy() throws AnalyticsException {
        if (this.indexer != null) {
            this.indexer.close();
        }
    }

}
