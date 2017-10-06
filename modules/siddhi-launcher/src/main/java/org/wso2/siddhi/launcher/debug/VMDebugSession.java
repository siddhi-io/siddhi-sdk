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
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.launcher.debug.dto.BreakPointDTO;
import org.wso2.siddhi.launcher.exception.DebugException;
import org.wso2.siddhi.launcher.internal.DebugRuntime;

import java.util.ArrayList;

/**
 * {@code VMDebugSession} The Debug Session class will be used to hold context for each client.
 * Each client will get its own instance of debug session.
 *
 */
public class VMDebugSession {

    private Channel channel = null;

    private DebugRuntime debugRuntime=null;

    public DebugRuntime getDebugRuntime() {
        return debugRuntime;
    }

    public void setDebugRuntime(DebugRuntime debugRuntime) {
        this.debugRuntime = debugRuntime;
    }

    public VMDebugSession() {

    }

    /**
     * Sets debug points.
     *
     * @param breakPoints the debug points
     */
    public void addDebugPoints(ArrayList<BreakPointDTO> breakPoints, String type) {
        if(type.equalsIgnoreCase("IN")){
            debugRuntime.getInBreakpointsMap().clear();
            for (BreakPointDTO breakPoint : breakPoints) {
                setBreakPoint(breakPoint);
            }
        }else if(type.equalsIgnoreCase("OUT")){
            debugRuntime.getOutBreakpointsMap().clear();
            for (BreakPointDTO breakPoint : breakPoints) {
                setBreakPoint(breakPoint);
            }
        }

    }

    /**
     * Helper method to set debug point
     *
     * @param breakPointDTO specific breakpoint
     */
    private void setBreakPoint(BreakPointDTO breakPointDTO) {
        if(breakPointDTO!=null) {
            if(breakPointDTO.getFileName()!=null && !breakPointDTO.getFileName().isEmpty() && breakPointDTO
                    .getLineNumber()!=null){
                String receivedBreakpointFileName=breakPointDTO.getFileName();
                int receivedBreakpointLineNumber=breakPointDTO.getLineNumber();
                String currentDebugFileName=this.debugRuntime.getSiddhiAppFileName();
                //Checking whether the breakpoint is applicable for current debug file
                if(currentDebugFileName.equalsIgnoreCase(receivedBreakpointFileName)) {
                    Integer queryIndex = breakPointDTO.getQueryIndex();
                    String queryTerminal = breakPointDTO.getQueryTerminal();
                    if (queryIndex != null && queryTerminal != null && !queryTerminal.isEmpty()) {
                        // acquire only specified break point
                        SiddhiDebugger.QueryTerminal terminal = ("in".equalsIgnoreCase(queryTerminal)) ?
                                SiddhiDebugger.QueryTerminal.IN : SiddhiDebugger.QueryTerminal.OUT;
                        String queryName = (String) debugRuntime.getQueries().toArray()[queryIndex];
                        debugRuntime.getDebugger().acquireBreakPoint(queryName, terminal);
                        BreakPointInfo breakPointInfo=new BreakPointInfo(receivedBreakpointFileName,
                                receivedBreakpointLineNumber,queryIndex, queryTerminal);
                        if(queryTerminal.equalsIgnoreCase("in")){
                            debugRuntime.getInBreakpointsMap().put(queryIndex+queryTerminal,breakPointInfo);
                        }else{
                            debugRuntime.getOutBreakpointsMap().put(queryIndex+queryTerminal,breakPointInfo);
                        }
                    } //TODO:Handle the exceptions after this
                }
            }
        }
    }

    /**
     * Gets channel.
     *
     * @return the channel
     */
    public synchronized Channel getChannel() {
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
        debugRuntime.debug();
    }

    /**
     * Method to stop debugging process in all the threads.
     */
    public void stopDebug() {
        debugRuntime.stop();
    }

    /**
     * Method to clear the channel so that another debug session can connect.
     */
    public synchronized void clearSession() {
        this.channel.close();
        this.channel = null;
    }

    public void notifyComplete() {
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyComplete(this);
    }

    public void notifyExit() {//TODO: Add notify complete and notify exit
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyExit(this);
    }

    public void notifyHalt(BreakPointInfo breakPointInfo) {
        VMDebugManager debugManager = VMDebugManager.getInstance();
        debugManager.notifyDebugHit(this, breakPointInfo);
    }
}
