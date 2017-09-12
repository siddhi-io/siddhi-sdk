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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.netty.channel.Channel;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.core.debugger.SiddhiDebuggerClient;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.launcher.debug.dto.CommandDTO;
import org.wso2.siddhi.launcher.debug.dto.MessageDTO;
import org.wso2.siddhi.launcher.exception.SiddhiException;
import org.wso2.siddhi.launcher.internal.DebugProcessorService;
import org.wso2.siddhi.launcher.internal.DebugRuntime;
import org.wso2.siddhi.launcher.internal.EditorDataHolder;
import org.wso2.siddhi.query.compiler.SiddhiCompiler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code VMDebugManager} Manages debug sessions and handle debug related actions.
 */
public class VMDebugManager {
    /**
     * The Execution sem. used to block debugger till client connects.
     */
    private volatile Semaphore executionWaitSem;

    private VMDebugServer debugServer;

    private boolean debugEnabled;

    private InputHandler inputHandler=null;//TODO:remove hardcoded input handler

    /**
     * Object to hold debug session related context.
     */
    private VMDebugSession debugSession;

    private static VMDebugManager debugManagerInstance = null;

    private boolean debugManagerInitialized = false;

    public VMDebugSession getDebugSession() {
        return debugSession;
    }

    /**
     * Instantiates a new Debug manager.
     */
    private VMDebugManager() {
        executionWaitSem = new Semaphore(0);
        debugServer = new VMDebugServer();
        debugSession = new VMDebugSession();
    }

    /**
     * Debug manager singleton.
     *
     * @return DebugManager instance
     */
    public static VMDebugManager getInstance() {
        if (debugManagerInstance != null) {
            return debugManagerInstance;
        }
        return initialize();
    }

    private static synchronized VMDebugManager initialize() {
        if (debugManagerInstance == null) {
            debugManagerInstance = new VMDebugManager();
        }
        return debugManagerInstance;
    }

    /**
     * Initializes the debug manager single instance.
     */
    public void mainInit(String siddhiApp, String inputFile) { //TODO:Add synchronizes
        if (this.debugManagerInitialized) {
            throw new SiddhiException("Debugger instance already initialized");
        }

        EditorDataHolder.setDebugProcessorService(new DebugProcessorService());
        SiddhiManager siddhiManager = new SiddhiManager();
        EditorDataHolder.setSiddhiManager(siddhiManager);
        //DebugRuntime debugRuntime=new DebugRuntime(siddhiApp);

        //this.inputHandler = debugRuntime.getInputHandler("sensorStream");//TODO:remove hardcoded value


        String cseEventStream = "define stream cseEventStream (symbol string, price float, " +
                "volume int);";
        final String query = "@info(name = 'query 1')" +
                "from cseEventStream " +
                "select symbol, price, volume " +
                "insert into OutputStream; ";

        DebugRuntime debugRuntime=new DebugRuntime(cseEventStream + query);
        EditorDataHolder.setDebugRuntime(debugRuntime);

        // start the debug server if it is not started yet.
        this.debugServer.startServer();
        this.debugManagerInitialized = true;
    }

    /**
     * Process debug command.
     *
     * @param json the json
     */
    public void processDebugCommand(String json) {
        try {
            processCommand(json);
        } catch (Exception e) {
            MessageDTO message = new MessageDTO();
            message.setCode(DebugConstants.CODE_INVALID);
            message.setMessage(e.getMessage());
            debugServer.pushMessageToClient(debugSession, message);
        }
    }

    private void processCommand(String json) {
        ObjectMapper mapper = new ObjectMapper();
        CommandDTO command = null;
        try {
            command = mapper.readValue(json, CommandDTO.class);
        } catch (IOException e) {
            //invalid message will be passed
            throw new DebugException(DebugConstants.MSG_INVALID);
        }

        switch (command.getCommand()) {
            case DebugConstants.CMD_RESUME:
                EditorDataHolder
                        .getDebugRuntime()
                        .getDebugger()
                        .play();
                break;
            case DebugConstants.CMD_STEP_OVER:
                EditorDataHolder
                        .getDebugRuntime()
                        .getDebugger()
                        .next();
                break;
            case DebugConstants.CMD_STOP:
                // When stopping the debug session, it will clear all debug points and resume all threads.
                debugSession.stopDebug();
                debugSession.clearSession();
                break;
            case DebugConstants.CMD_SET_POINTS://TODO:Control the adding breakpoints
                // we expect { "command": "SET_POINTS", points: [{ "fileName": "sample.bal", "lineNumber" : 5 },{...}]}
                debugSession.addDebugPoints(command.getPoints());
                sendAcknowledge(this.debugSession, "Debug points updated");
                break;
            case DebugConstants.CMD_START:
                // Client needs to explicitly start the execution once connected.
                // This will allow client to set the breakpoints before starting the execution.
                sendAcknowledge(this.debugSession, "Debug started.");
                debugSession.startDebug();
                this.inputHandler = EditorDataHolder.getDebugRuntime().getInputHandler("cseEventStream");
                try {
                    inputHandler.send(new Object[]{"tempID1",99.8});
                } catch (InterruptedException e) {
                    throw new DebugException(e.getMessage());
                }
                break;
            default:
                throw new DebugException(DebugConstants.MSG_INVALID);
        }
    }

    /**
     * Set debug channel.
     *
     * @param channel the channel
     */
    public void addDebugSession(Channel channel) throws DebugException {
        this.debugSession.setChannel(channel);
        sendAcknowledge(this.debugSession, "Channel registered.");
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void releaseExecutionLock() {
        this.executionWaitSem.release();
    }

    public boolean isDebugSessionActive() {
        return (this.debugSession.getChannel() != null);
    }


    /**
     * Send a message to the debug client when a breakpoint is hit.
     *
     * @param debugSession current debugging session
     * //@param breakPointInfo info of the current break point
     */
    public void notifyDebugHit(VMDebugSession debugSession){//}, BreakPointInfo breakPointInfo) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_HIT);
        message.setMessage(DebugConstants.MSG_HIT);
//        message.setThreadId(breakPointInfo.getThreadId());
//        message.setLocation(breakPointInfo.getHaltLocation());
//        message.setFrames(breakPointInfo.getCurrentFrames());
        debugServer.pushMessageToClient(debugSession, message);
    }


    /**
     * Notify client when debugger has finish execution.
     *
     * @param debugSession current debugging session
     */
    public void notifyComplete(VMDebugSession debugSession) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_COMPLETE);
        message.setMessage(DebugConstants.MSG_COMPLETE);
        debugServer.pushMessageToClient(debugSession, message);
    }

    /**
     * Notify client when the debugger is exiting.
     *
     * @param debugSession current debugging session
     */
    public void notifyExit(VMDebugSession debugSession) {
        if (!isDebugSessionActive()) {
            return;
        }
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_EXIT);
        message.setMessage(DebugConstants.MSG_EXIT);
        debugServer.pushMessageToClient(debugSession, message);
    }

    /**
     * Send a generic acknowledge message to the client.
     *
     * @param debugSession current debugging session
     * @param messageText message to send to the client
     */
    public void sendAcknowledge(VMDebugSession debugSession, String messageText) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_ACK);
        message.setMessage(messageText);
        debugServer.pushMessageToClient(debugSession, message);
    }
}
