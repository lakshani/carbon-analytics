/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.analytics.datasink.internal.queue;

import org.wso2.carbon.databridge.commons.Event;

import java.util.concurrent.ConcurrentHashMap;

public class AnalyticsEventQueueManager {

    private static AnalyticsEventQueueManager instance = new AnalyticsEventQueueManager();
    private static ConcurrentHashMap<Integer, AnalyticsEventQueue> queueMap =
            new ConcurrentHashMap<Integer, AnalyticsEventQueue>();

    private AnalyticsEventQueueManager() {
    }

    public static AnalyticsEventQueueManager getInstance() {
        return instance;
    }

    public void put(int tenantId, Event event) {
        AnalyticsEventQueue eventQueue = queueMap.get(tenantId);
        if (eventQueue == null) {
            synchronized (this) {
                eventQueue = queueMap.get(tenantId);
                if (eventQueue == null) {
                    eventQueue = new AnalyticsEventQueue(tenantId);
                    queueMap.put(tenantId, eventQueue);
                }
            }
        }
        eventQueue.put(event);
    }
}
