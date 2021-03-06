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
package org.wso2.carbon.analytics.datasource.core.rs;

import java.util.List;

import org.wso2.carbon.analytics.datasource.core.AnalyticsException;

/**
 * {@link RecordGroup} implementation for direct analytics data source.
 */
public class DirectRecordGroup implements RecordGroup {

    private static final long serialVersionUID = -1195668816256005842L;

    private static final String LOCALHOST = "localhost";
    
    private boolean withIds;
    
    private int tenantId;
    
    private String tableName;
    
    private List<String> columns;
    
    private long timeFrom;
    
    private long timeTo;
    
    private int recordsFrom;
    
    private int recordsCount;
    
    private List<String> ids;
    
    public DirectRecordGroup(int tenantId, String tableName, List<String> columns, List<String> ids) {
        this.withIds = true;
        this.tenantId = tenantId;
        this.tableName = tableName;
        this.columns = columns;
        this.ids = ids;
    }
    
    public DirectRecordGroup(int tenantId, String tableName, List<String> columns, long timeFrom, 
            long timeTo, int recordsFrom, int recordsCount) {
        this.withIds = false;
        this.tenantId = tenantId;
        this.tableName = tableName;
        this.columns = columns;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.recordsFrom = recordsFrom;
        this.recordsCount = recordsCount;
    }
    
    @Override
    public String[] getLocations() throws AnalyticsException {
        return new String[] { LOCALHOST };
    }

    public boolean isWithIds() {
        return withIds;
    }
    
    public int getTenantId() {
        return tenantId;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public long getTimeFrom() {
        return timeFrom;
    }
    
    public long getTimeTo() {
        return timeTo;
    }
    
    public int getRecordsFrom() {
        return recordsFrom;
    }
    
    public int getRecordsCount() {
        return recordsCount;
    }
    
    public List<String> getIds() {
        return ids;
    }
    
}