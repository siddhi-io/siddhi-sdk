/*
 * Copyright (c)  2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.siddhi.launcher.internal;

import org.wso2.siddhi.launcher.debug.BreakPointInfo;
import org.wso2.siddhi.launcher.debug.VMDebugManager;
import org.wso2.siddhi.launcher.exception.InvalidExecutionStateException;
import org.wso2.siddhi.launcher.exception.NoSuchStreamException;
import org.wso2.siddhi.launcher.util.DebugCallbackEvent;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class DebugRuntime {
    private Mode mode = Mode.STOP;
    private transient String siddhiApp;

    private transient String siddhiAppFileName;

    private transient SiddhiAppRuntime siddhiAppRuntime;
    private transient SiddhiDebugger debugger;
    private transient LinkedBlockingQueue<DebugCallbackEvent> callbackEventsQueue;


    public String getSiddhiAppFileName() {
        return siddhiAppFileName;
    }

    public DebugRuntime(String siddhiAppFileName, String siddhiApp) {
        this.siddhiApp = siddhiApp;
        this.siddhiAppFileName=siddhiAppFileName;
        callbackEventsQueue = new LinkedBlockingQueue<>(10);
        createRuntime();
    }

    public SiddhiDebugger getDebugger() {
        return debugger;
    }

    /**
     * Print information message to the console.
     *
     * @param msg the message
     */
    private static void info(String msg) {
        System.out.println("INFO: " + msg);
    }//TODO:Remove this

    public void debug() {
        if (Mode.STOP.equals(mode)) {
                debugger = siddhiAppRuntime.debug();
                debugger.setDebuggerCallback((event, queryName, queryTerminal, debugger) -> {
                    String[] queries = getQueries().toArray(new String[getQueries().size()]);
                    int queryIndex = Arrays.asList(queries).indexOf(queryName);
                    callbackEventsQueue.add(new DebugCallbackEvent(queryName, queryIndex, queryTerminal, event));
                    //Sending message to client on debug hit
                    info("@Debug: Query: " + queryName + ", Terminal: " + queryTerminal + ", Event: " + event);
                    Map<String, Object> queryState =EditorDataHolder.getDebugRuntime().getDebugger().getQueryState
                            (queryName);
                    BreakPointInfo breakPointInfo=new BreakPointInfo(queryIndex,queryTerminal.toString(),queryState);
                    VMDebugManager.getInstance().getDebugSession().notifyHalt(breakPointInfo);

                });
                mode = Mode.DEBUG;
        } else if (Mode.FAULTY.equals(mode)) {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        } else {
            throw new InvalidExecutionStateException("Siddhi App is already running.");
        }
    }

    public void stop() {
        if (debugger != null) {
            debugger.releaseAllBreakPoints();
            debugger.play();
            debugger = null;
        }
        if (siddhiAppRuntime != null) {
            siddhiAppRuntime.shutdown();
            siddhiAppRuntime = null;
        }
        callbackEventsQueue.clear();
        createRuntime();
    }

    public List<String> getStreams() {
        if (!Mode.FAULTY.equals(mode)) {
            return new ArrayList<>(siddhiAppRuntime.getStreamDefinitionMap().keySet());
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public List<String> getQueries() {
        if (!Mode.FAULTY.equals(mode)) {
            return new ArrayList<>(siddhiAppRuntime.getQueryNames());
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public InputHandler getInputHandler(String streamName) {
        if (!Mode.FAULTY.equals(mode)) {
            return siddhiAppRuntime.getInputHandler(streamName);
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public List<Attribute> getStreamAttributes(String streamName) {
        if (!Mode.FAULTY.equals(mode)) {
            if (siddhiAppRuntime.getStreamDefinitionMap().containsKey(streamName)) {
                return siddhiAppRuntime.getStreamDefinitionMap().get(streamName).getAttributeList();
            } else {
                throw new NoSuchStreamException(
                        "Stream definition %s does not exists in Siddhi app "+ streamName);
            }
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public LinkedBlockingQueue<DebugCallbackEvent> getCallbackEventsQueue() {
        return callbackEventsQueue;
    }

    private void createRuntime() {
            if (siddhiApp != null && !siddhiApp.isEmpty()) {
                siddhiAppRuntime = EditorDataHolder.getSiddhiManager()
                        .createSiddhiAppRuntime(siddhiApp);
                mode = Mode.STOP;
            } else {
                mode = Mode.FAULTY;
            }
    }

    private enum Mode { DEBUG, STOP, FAULTY}

}
