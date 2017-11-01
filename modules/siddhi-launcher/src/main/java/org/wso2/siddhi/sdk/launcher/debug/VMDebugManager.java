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

package org.wso2.siddhi.sdk.launcher.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.sdk.launcher.debug.dto.CommandDTO;
import org.wso2.siddhi.sdk.launcher.debug.dto.MessageDTO;
import org.wso2.siddhi.sdk.launcher.debug.internal.DebugRuntime;
import org.wso2.siddhi.sdk.launcher.exception.DebugException;
import org.wso2.siddhi.sdk.launcher.util.InputFeeder;

import java.io.File;
import java.io.IOException;

/**
 * {@code VMDebugManager} Manages debug sessions and handle debug related actions.
 */
public class VMDebugManager {

    private static final Logger log = Logger.getLogger(VMDebugManager.class);

    private static VMDebugManager debugManagerInstance = null;
    private static SiddhiManager siddhiManager = new SiddhiManager();
    private VMDebugServer debugServer;
    /**
     * Object to hold debug session related context.
     */
    private VMDebugSession debugSession;
    private boolean debugManagerInitialized = false;
    private InputFeeder inputFeeder = null;

    /**
     * Instantiates a new Debug manager.
     */
    private VMDebugManager() {
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

    public VMDebugSession getDebugSession() {
        return debugSession;
    }

    public SiddhiManager getSiddhiManager() {
        return siddhiManager;
    }

    /**
     * Initializes the debug manager single instance.
     */
    public void mainInit(String siddhiAppPath, String siddhiApp, String inputFile) {
        if (debugManagerInitialized) {
            throw new DebugException("Debugger instance already initialized");
        }
        File f = new File(siddhiAppPath);
        String fileName = f.getName();
        DebugRuntime debugRuntime = new DebugRuntime(fileName, siddhiApp);
        debugSession.setDebugRuntime(debugRuntime);
        if (!(inputFile == null || inputFile.equalsIgnoreCase(""))) {
            inputFeeder = new InputFeeder(debugRuntime.getSiddhiAppRuntime(), inputFile);
        }
        // start the debug server if it is not started yet.
        debugServer.startServer();
        debugManagerInitialized = true;
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
                debugSession
                        .getDebugRuntime()
                        .getDebugger()
                        .play();
                break;
            case DebugConstants.CMD_STEP_OVER:
                debugSession
                        .getDebugRuntime()
                        .getDebugger()
                        .next();
                break;
            case DebugConstants.CMD_STOP:
                if (inputFeeder != null) {
                    inputFeeder.stop();
                }
                // When stopping the debug session, it will clear all debug points and resume all threads.
                debugSession.stopDebug();
                debugSession.clearSession();
                break;
            case DebugConstants.CMD_SET_POINTS:
                // we expect { "command": "SET_POINTS",points: [{ "fileName": "sample.siddhi",
                // "lineNumber" : 5,"queryIndex": 0, "queryTerminal": "IN" }, {...}]}
                debugSession.addDebugPoints(command.getPoints());
                sendAcknowledge(debugSession, "Debug points updated");
                break;
            case DebugConstants.CMD_REMOVE_BREAKPOINT:
                // we expect { "command": "REMOVE_BREAKPOINT",points: [{ "fileName": "sample.siddhi",
                // "lineNumber" : 5,"queryIndex": 0, "queryTerminal": "IN" }]}
                debugSession.removeDebugPoints(command.getPoints());
                sendAcknowledge(debugSession, "Debug point removed");
                break;
            case DebugConstants.CMD_SEND_EVENT:
                if (inputFeeder != null) {
                    inputFeeder.start();
                    sendAcknowledge(debugSession, "Input feeder started.");
                } else {
                    log.info("Input file is empty or null");
                }
                break;
            case DebugConstants.CMD_START:
                // Client needs to explicitly start the execution once connected.
                debugSession.startDebug();
                sendAcknowledge(debugSession, "Debug started.");
                break;
            default:
                throw new DebugException(DebugConstants.MSG_INVALID);
                //TODO:Add debug logs wherever possible. ex:when reaching a breakpoint,sending/receive commands etc.
        }
    }

    /**
     * Set debug channel.
     *
     * @param channel the channel
     */
    public void addDebugSession(Channel channel) throws DebugException {
        debugSession.setChannel(channel);
        sendAcknowledge(debugSession, "Channel registered.");
    }

    private boolean isDebugSessionActive() {
        return (debugSession.getChannel() != null);
    }

    /**
     * Send a message to the debug client when a breakpoint is hit.
     *
     * @param debugSession   current debugging session
     * @param breakPointInfo info of the current break point
     */
    public void notifyDebugHit(VMDebugSession debugSession, BreakPointInfo breakPointInfo) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_HIT);
        message.setMessage(DebugConstants.MSG_HIT);
        message.setEventInfo(breakPointInfo.getEventInfo());
        message.setQueryName(breakPointInfo.getQueryName());
        message.setQueryState(breakPointInfo.getQueryState());

        String fileName = breakPointInfo.getFileName();
        int queryIndex = breakPointInfo.getQueryIndex();
        String queryTerminal = breakPointInfo.getQueryTerminal();

        message.setLocation(fileName, queryIndex, queryTerminal);
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
     * @param messageText  message to send to the client
     */
    private void sendAcknowledge(VMDebugSession debugSession, String messageText) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_ACK);
        message.setMessage(messageText);
        debugServer.pushMessageToClient(debugSession, message);
    }
}
