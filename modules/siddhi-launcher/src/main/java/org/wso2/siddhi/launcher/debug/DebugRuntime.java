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

package org.wso2.siddhi.launcher.debug;

import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.launcher.debug.util.DebugCallbackEvent;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.launcher.exception.InvalidExecutionStateException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class DebugRuntime {
    private Mode mode = Mode.STOP;
    private transient String siddhiApp;
    private transient SiddhiAppRuntime siddhiAppRuntime;
    private transient SiddhiDebugger debugger;
    private transient LinkedBlockingQueue<DebugCallbackEvent> callbackEventsQueue;//todo: Check the highlighted error

    public DebugRuntime(String siddhiApp) {
        this.siddhiApp = siddhiApp;
        callbackEventsQueue = new LinkedBlockingQueue<>(10);
        createRuntime();
    }

    public void debug() {
        if (Mode.STOP.equals(mode)) {
            try {
                debugger = siddhiAppRuntime.debug();
                debugger.setDebuggerCallback((event, queryName, queryTerminal, debugger) -> {
                    String[] queries = getQueries().toArray(new String[getQueries().size()]);
                    int queryIndex = Arrays.asList(queries).indexOf(queryName);
                    callbackEventsQueue.add(new DebugCallbackEvent(queryName, queryIndex, queryTerminal, event));
                });
                mode = Mode.DEBUG;
            }catch (RuntimeException e){
                mode =Mode.FAULTY;
            } catch (Exception e) {//TODO: Handle the exception in a proper manner
                mode = Mode.FAULTY;
            }
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

    public List<String> getQueries() {
        if (!Mode.FAULTY.equals(mode)) {
            return new ArrayList<>(siddhiAppRuntime.getQueryNames());
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    private void createRuntime() {
        try {
            if (siddhiApp != null && !siddhiApp.isEmpty()) {
                // Creating Siddhi Manager
                SiddhiManager siddhiManager = new SiddhiManager();

                //Generating runtime
                siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

                mode = Mode.STOP;
            } else {
                mode = Mode.FAULTY;
            }
        } catch (RuntimeException e) {
            mode = Mode.FAULTY;
        }catch (Exception e){
            mode = Mode.FAULTY;
        }
    }

    private enum Mode {DEBUG, STOP, FAULTY}

}
