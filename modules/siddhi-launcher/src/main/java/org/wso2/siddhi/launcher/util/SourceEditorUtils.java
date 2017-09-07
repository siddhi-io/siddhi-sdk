/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.siddhi.launcher.util;

import org.apache.log4j.Logger;
import org.wso2.siddhi.launcher.internal.EditorDataHolder;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for getting the meta data for the in built and extension processors in siddhi
 */
public class SourceEditorUtils {
    static final Logger LOGGER = Logger.getLogger(SourceEditorUtils.class);

    private SourceEditorUtils() {

    }

    /**
     * Validate the siddhi app string using the Siddhi Manager
     * Will return a valid siddhiAppRuntime
     *
     * @param siddhiApp Siddhi app string
     * @return Valid siddhi app runtime
     */
    public static SiddhiAppRuntime validateSiddhiApp(String siddhiApp) {
        SiddhiAppRuntime siddhiAppRuntime = null;
        try {
            siddhiAppRuntime = EditorDataHolder.getSiddhiManager().createSiddhiAppRuntime(siddhiApp);
            siddhiAppRuntime.start();
        } finally {
            if (siddhiAppRuntime != null) {
                siddhiAppRuntime.shutdown();
            }
        }
        return siddhiAppRuntime;
    }

    /**
     * Get the definition of the inner streams in the partitions
     * Inner streams will be separated based on the partition
     *
     * @param siddhiAppRuntime              Siddhi app runtime created after validating
     * @param partitionsWithMissingInnerStreams Required inner stream names separated based on partition it belongs to
     * @return The inner stream definitions separated base on the partition it belongs to
     */
    public static List<List<AbstractDefinition>> getInnerStreamDefinitions(SiddhiAppRuntime siddhiAppRuntime,
                                                                           List<List<String>>
                                                                                   partitionsWithMissingInnerStreams) {
        List<List<AbstractDefinition>> innerStreamDefinitions = new ArrayList<>();

        // Transforming the element ID to partition inner streams map to element ID no to partition inner streams map
        Map<Integer, Map<String, AbstractDefinition>> innerStreamsMap = new ConcurrentHashMap<>();
        siddhiAppRuntime.getPartitionedInnerStreamDefinitionMap().entrySet().parallelStream().forEach(
                entry -> innerStreamsMap.put(
                        Integer.valueOf(entry.getKey().split("-")[1]),
                        entry.getValue()
                )
        );

        // Creating an ordered list of partition inner streams based on partition element ID
        // This is important since the client sends the missing inner streams 2D list
        // with partitions in the order they are in the siddhi app
        List<Map<String, AbstractDefinition>> rankedPartitionsWithInnerStreams = new ArrayList<>();
        List<Integer> rankedPartitionElementIds = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, AbstractDefinition>> entry :
                innerStreamsMap.entrySet()) {
            int i = 0;
            for (; i < rankedPartitionsWithInnerStreams.size(); i++) {
                if (entry.getKey() < rankedPartitionElementIds.get(i)) {
                    break;
                }
            }
            rankedPartitionsWithInnerStreams.add(i, entry.getValue());
            rankedPartitionElementIds.add(i, entry.getKey());
        }

        // Extracting the requested stream definitions from based on the order
        // in rankedPartitionsWithInnerStreams and partitionsWithMissingInnerStreams
        // The inner stream definitions 2D list fetched from the Siddhi Manager
        // and the missing inner streams 2D list are now in the same order
        // Therefore the outer loops in both lists can be looped together
        for (int i = 0; i < partitionsWithMissingInnerStreams.size(); i++) {
            List<String> partitionWithMissingInnerStreams = partitionsWithMissingInnerStreams.get(i);
            Map<String, AbstractDefinition> partitionWithInnerStreams = rankedPartitionsWithInnerStreams.get(i);
            List<AbstractDefinition> innerStreamDefinition = new ArrayList<>();

            for (String missingInnerStream : partitionWithMissingInnerStreams) {
                AbstractDefinition streamDefinition = partitionWithInnerStreams.get(missingInnerStream);
                if (streamDefinition != null) {
                    innerStreamDefinition.add(streamDefinition);
                }
            }
            innerStreamDefinitions.add(innerStreamDefinition);
        }

        return innerStreamDefinitions;
    }

    /**
     * Get the definitions of the streams that are requested
     * used for fetching the definitions of streams that queries output into without defining them first
     *
     * @param siddhiAppRuntime Siddhi app runtime created after validating
     * @param missingStreams       Required stream names
     * @return The stream definitions
     */
    public static List<AbstractDefinition> getStreamDefinitions(SiddhiAppRuntime siddhiAppRuntime,
                                                                List<String> missingStreams) {
        List<AbstractDefinition> streamDefinitions = new ArrayList<>();
        Map<String, StreamDefinition> streamDefinitionMap = siddhiAppRuntime.getStreamDefinitionMap();
        for (String stream : missingStreams) {
            AbstractDefinition streamDefinition = streamDefinitionMap.get(stream);
            if (streamDefinition != null) {
                streamDefinitions.add(streamDefinition);
            }
        }
        return streamDefinitions;
    }


}
