/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/


package org.wso2.siddhi.launcher.debug;

import io.netty.channel.Channel;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.nonblocking.debugger.BreakPointInfo;
import org.ballerinalang.bre.nonblocking.debugger.DebugSessionObserver;
import org.ballerinalang.util.debugger.dto.BreakPointDTO;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.launcher.internal.EditorDataHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * {@code VMDebugSession} The Debug Session class will be used to hold context for each client.
 * Each client will get its own instance of debug session.
 *
 * @since 0.88
 */
public class VMDebugSession {

    private Channel channel = null;

    private ArrayList<BreakPointDTO> breakPoints;

    //key - threadid
    private Map<String, Context> contextMap;

    public VMDebugSession() {
        this.contextMap = new HashMap<>();
    }

    /**
     * Sets debug points.
     *
     * @param breakPoints the debug points
     */
    public void addDebugPoints(ArrayList<BreakPointDTO> breakPoints) {
        this.breakPoints = breakPoints;
        for (Context bContext : this.contextMap.values()) {
            setBreakPoints(bContext);
        }
    }

    /**
     * Helper method to set debug points to the given context.
     *
     * @param bContext
     */
    private void setBreakPoints(String siddhiAppName,Integer queryIndex,String queryTerminal) {
        if (queryIndex != null && queryTerminal != null && !queryTerminal.isEmpty()) {
            // acquire only specified break point
            SiddhiDebugger.QueryTerminal terminal = ("in".equalsIgnoreCase(queryTerminal)) ?
                    SiddhiDebugger.QueryTerminal.IN : SiddhiDebugger.QueryTerminal.OUT;
            String queryName = (String) EditorDataHolder
                    .getDebugProcessorService()
                    .getSiddhiAppRuntimeHolder(siddhiAppName)
                    .getQueries()
                    .toArray()[queryIndex];
            EditorDataHolder
                    .getDebugProcessorService()
                    .getSiddhiAppRuntimeHolder(siddhiAppName)
                    .getDebugger()
                    .acquireBreakPoint(queryName, terminal);
            return Response
                    .status(Response.Status.OK)
                    .entity(new GeneralResponse(Status.SUCCESS, "Terminal " + queryTerminal +
                            " breakpoint acquired for query " + siddhiAppName + ":" + queryName))
                    .build();
        } else {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new GeneralResponse(Status.ERROR, "Missing Parameters"))
                    .build();
        }
    }

    /**
     * Gets channel.
     *
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    public synchronized void setChannel(Channel channel) throws DebugException {
        if (this.channel != null) {
            throw new DebugException("Debug session already exist");
        }
        this.channel = channel;
    }

    /**
     * Method to start debugging process in all the threads.
     */
    public void startDebug() {
        List<String> streams = EditorDataHolder
                .getDebugProcessorService()
                .getSiddhiAppRuntimeHolder(siddhiAppName)
                .getStreams();
        List<String> queries = EditorDataHolder
                .getDebugProcessorService()
                .getSiddhiAppRuntimeHolder(siddhiAppName)
                .getQueries();
        EditorDataHolder
                .getDebugProcessorService()
                .debug(siddhiAppName);
    }

    /**
     * Method to stop debugging process in all the threads.
     */
    public void stopDebug() {
        EditorDataHolder
                .getDebugProcessorService()
                .stop(siddhiAppName);
    }

    /**
     * Method to clear the channel so that another debug session can connect.
     */
    public void clearSession() {
        this.channel.close();
        this.channel = null;
    }

    @Override
    public void notifyComplete() {
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyComplete(this);
    }

    @Override
    public void notifyExit() {
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyExit(this);
    }

    @Override
    public void notifyHalt(BreakPointInfo breakPointInfo) {
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyDebugHit(this, breakPointInfo);
    }
}
