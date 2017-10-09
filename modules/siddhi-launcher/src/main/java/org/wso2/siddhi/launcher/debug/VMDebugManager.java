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
import org.wso2.siddhi.launcher.debug.dto.CommandDTO;
import org.wso2.siddhi.launcher.debug.dto.MessageDTO;
import org.wso2.siddhi.launcher.exception.DebugException;
import org.wso2.siddhi.launcher.exception.SiddhiException;
import org.wso2.siddhi.launcher.internal.DebugRuntime;
import org.wso2.siddhi.launcher.util.PrintInfo;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code VMDebugManager} Manages debug sessions and handle debug related actions.
 */
public class VMDebugManager {

    private VMDebugServer debugServer;

    /**
     * Object to hold debug session related context.
     */
    private VMDebugSession debugSession;

    private static VMDebugManager debugManagerInstance = null;

    private boolean debugManagerInitialized = false;

    private static SiddhiManager siddhiManager=new SiddhiManager();

    private InputFeeder inputFeeder =null;

    public VMDebugSession getDebugSession() {
        return debugSession;
    }

    public SiddhiManager getSiddhiManager() {
        return siddhiManager;
    }

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

    /**
     * Initializes the debug manager single instance.
     */
    public void mainInit(String siddhiAppPath,String siddhiApp, String inputFile) {
        if (this.debugManagerInitialized) {
            throw new SiddhiException("Debugger instance already initialized");
        }
        File f = new File(siddhiAppPath);
        String fileName=f.getName();
        DebugRuntime debugRuntime=new DebugRuntime(fileName, siddhiApp);
        debugSession.setDebugRuntime(debugRuntime);
        if(!(inputFile==null || inputFile.equalsIgnoreCase(""))){
            inputFeeder=new InputFeeder(debugRuntime.getSiddhiAppRuntime(), inputFile);
        }
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
                if(inputFeeder!=null) {
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
                sendAcknowledge(this.debugSession, "Debug points updated");
                break;
            case DebugConstants.CMD_REMOVE_BREAKPOINT:
                // we expect { "command": "REMOVE_BREAKPOINT",points: [{ "fileName": "sample.siddhi",
                // "lineNumber" : 5,"queryIndex": 0, "queryTerminal": "IN" }]}
                debugSession.removeDebugPoints(command.getPoints());
                sendAcknowledge(this.debugSession, "Debug point removed");
                break;
            case DebugConstants.CMD_SEND_EVENT:
                if(inputFeeder!=null){
                    inputFeeder.start();
                    sendAcknowledge(this.debugSession, "Input feeder started.");
                }else{
                    PrintInfo.info("Error: Input file is empty or null");
                }
                break;
            case DebugConstants.CMD_START:
                // Client needs to explicitly start the execution once connected.
                debugSession.startDebug();
                sendAcknowledge(this.debugSession, "Debug started.");
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

    public boolean isDebugSessionActive() {
        return (this.debugSession.getChannel() != null);
    }

    /**
     * Send a message to the debug client when a breakpoint is hit.
     *
     * @param debugSession current debugging session
     * //@param breakPointInfo info of the current break point
     */
    public void notifyDebugHit(VMDebugSession debugSession , BreakPointInfo breakPointInfo) {
        MessageDTO message = new MessageDTO();
        message.setCode(DebugConstants.CODE_HIT);
        message.setMessage(DebugConstants.MSG_HIT);
        message.setEventInfo(breakPointInfo.getEventInfo());
        message.setQueryName(breakPointInfo.getQueryName());
        message.setQueryState(breakPointInfo.getQueryState());

        String fileName=breakPointInfo.getFileName();
        int queryIndex=breakPointInfo.getQueryIndex();
        String queryTerminal=breakPointInfo.getQueryTerminal();

        message.setLocation(fileName,queryIndex,queryTerminal);
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

    /**
     * Method name used to specify delay in input.
     * Example: delay(100)
     */
    private static final String DELAY = "delay";

    /**
     * Delimiter separating stream name and the input data in the input.
     */
    private static final String INPUT_DELIMITER = "=";

    /**
     * A runnable class to feed the input to the Siddhi runtime.
     */
    private static class InputFeeder implements Runnable {
        private final SiddhiAppRuntime siddhiAppRuntime;
        private String input;
        private volatile AtomicBoolean running = new AtomicBoolean(false);
        private Thread thread;

        private InputFeeder(SiddhiAppRuntime siddhiAppRuntime, String input) {
            this.siddhiAppRuntime = siddhiAppRuntime;
            this.input = input;
        }

        @Override
        public void run() {
            // Scanner to read the user input line by line
            Scanner scanner = new Scanner(input);
            Gson gson = new Gson();
            while (scanner.hasNext()) {
                if (!running.get()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.startsWith(DELAY)) {
                    // The delay(<time in milliseconds>) is used to delay the input
                    line = line.substring(6, line.length() - 1);
                    try {
                        Thread.sleep(Integer.parseInt(line));
                    } catch (InterruptedException e) {
                        PrintInfo.info("ERROR: "+"Error in waiting for " + line + " milliseconds");
                    }
                } else {
                    // The inout format is: <stream name>=<data in json object[] format>
                    String[] components = line.split(INPUT_DELIMITER);
                    String streamName = components[0];
                    String event = components[1];
                    Object[] data = gson.fromJson(event, Object[].class);
                    PrintInfo.info("@Send: Stream: " + streamName + ", Event: " + event);
                    try {
                        siddhiAppRuntime.getInputHandler(streamName).send(data);
                    } catch (InterruptedException e) {
                        PrintInfo.info("ERROR: "+"Error in sending event " + event + " to Siddhi");
                    }
                }
            }
            scanner.close();
        }

        /**
         * Check whether the input feeder is running or not.
         *
         * @return true if the input feeder is running or not stopped manually, otherwise false.
         */
        boolean isRunning() {
            return running.get();

        }

        /**
         * Stop the input feeder.
         */
        void stop() {
            this.running.set(false);
        }

        /**
         * Start the input feeder.
         */
        public void start() {
            if (!this.running.get()) {
                this.running.set(true);
                thread = new Thread(this);
                thread.start();
            }
        }

        /**
         * Join the current thread behind the thread used to execute the input feeder.
         */
        public void join() {
            try {
                this.thread.join();
            } catch (InterruptedException e) {
                PrintInfo.info("ERROR: "+"Error in joining the main thread behind the input feeder");
            }
        }
    }
}
