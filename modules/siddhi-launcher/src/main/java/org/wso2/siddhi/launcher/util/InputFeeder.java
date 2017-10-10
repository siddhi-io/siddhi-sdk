/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.siddhi.launcher.util;


import com.google.gson.Gson;
import org.wso2.siddhi.core.SiddhiAppRuntime;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A runnable class to feed the input to the Siddhi runtime.
 */
public class InputFeeder implements Runnable {

    /**
     * Method name used to specify delay in input.
     * Example: delay(100)
     */
    private static final String DELAY = "delay";

    /**
     * Delimiter separating stream name and the input data in the input.
     */
    private static final String INPUT_DELIMITER = "=";

    private final SiddhiAppRuntime siddhiAppRuntime;
    private String input;
    private volatile AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public InputFeeder(SiddhiAppRuntime siddhiAppRuntime, String input) {
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
                    PrintInfo.error("Error in waiting for " + line + " milliseconds");
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
                    PrintInfo.error("Error in sending event " + event + " to Siddhi");
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
    public boolean isRunning() {
        return running.get();

    }

    /**
     * Stop the input feeder.
     */
    public void stop() {
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
            PrintInfo.error("Error in joining the main thread behind the input feeder");
        }
    }
}
