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
package org.wso2.siddhi.runner;

import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;

import java.io.*;

/**
 * This is the main class for starting siddhi and debugging siddhi
 */
public class SiddhiRun {
    public static void main(String []args) {

        //TODO: Fix the skip runtime in pom file

        // Validate the number of arguments
        if (args.length != 2) {
            error("Expected one argument but found " + args.length + "\n. Please try again with a valid argument: " +
                    "<siddhi file>");
            return;
        }

        String runningMethod = args[0];

        if(runningMethod.equalsIgnoreCase("run")){
            String siddhiAppPath = args[1];
            try {
                runSiddhi(siddhiAppPath);
            } catch (InterruptedException e) {
                e.printStackTrace();//TODO: Handle this in a proper way
            }
            return;
        }if(runningMethod.equalsIgnoreCase("debug")){
            debugSiddhi();
        }
    }

    private static void runSiddhi(String siddhiAppPath) throws InterruptedException {
        // Validate file
        File siddhiAppFile = new File(siddhiAppPath);
        if (!siddhiAppFile.exists() || !siddhiAppFile.isFile()) {
            error("Invalid siddhi app file: " + siddhiAppPath);
        }

        // Read the files
        String siddhiApp;
        try {
            siddhiApp = readText(siddhiAppPath);
        } catch (IOException e) {
            error("Failed to read " + siddhiAppPath);
            return;
        }
        // Creating Siddhi Manager
        SiddhiManager siddhiManager = new SiddhiManager();

        //Generating runtime
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        //Starting event processing
        siddhiAppRuntime.start();

        Thread.sleep(500);

        //Shutting down the runtime
        stopSiddhiAppRuntime(siddhiAppRuntime);

        //Shutting down Siddhi
        stopSiddhiManager(siddhiManager);
    }

    private static void debugSiddhi(){
        //DO Something
    }
    private static void stopSiddhiAppRuntime(SiddhiAppRuntime siddhiAppRuntime){
        //Shutting down the runtime
        siddhiAppRuntime.shutdown();
    }
    private static void stopSiddhiManager(SiddhiManager siddhiManager){
        //Shutting down the runtime
        siddhiManager.shutdown();
    }

    /**
     * Print error message to the console.
     *
     * @param msg the message
     */
    private static void error(String msg) {
        System.out.println("ERROR: " + msg);
    }

    /**
     * Read the text content of the given file.
     *
     * @param path the path of the file
     * @return the textr content of the file
     * @throws IOException if there are any issues in reading the file
     */
    private static String readText(String path) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (FileNotFoundException e) {
            error("The file " + path + " does not exist");
            throw e;
        } catch (IOException e) {
            error("Error in reading the file " + path);
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    error("Error when closing the input reader of " + path);
                }
            }
        }
    }
}
