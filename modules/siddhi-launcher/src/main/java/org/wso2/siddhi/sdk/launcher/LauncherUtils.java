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
package org.wso2.siddhi.sdk.launcher;

import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.sdk.launcher.debug.VMDebugManager;
import org.wso2.siddhi.sdk.launcher.exception.FileReadException;
import org.wso2.siddhi.sdk.launcher.exception.InvalidExecutionStateException;
import org.wso2.siddhi.sdk.launcher.exception.SLauncherException;
import org.wso2.siddhi.sdk.launcher.run.SiddhiRun;
import org.wso2.siddhi.sdk.launcher.util.PrintInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Contains utility methods for executing a Siddhi program.
 */
public class LauncherUtils {

    public static void runProgram(boolean isDebugEnabled, String[] args) throws FileReadException {

        if (args.length == 0 || args[0] == null) {
            throw new InvalidExecutionStateException("No Siddhi App Path given");
        }
        String siddhiAppPath = args[0];
        // Validate siddhiApp
        String siddhiApp = validateAndGetSiddhdiApp(siddhiAppPath);
        String inputFilePath;
        String inputFile = "";
        if (!(args.length == 1 || args[1] == null || args[1].equalsIgnoreCase(""))) {
            inputFilePath = args[1];
            try {
                inputFile = readText(inputFilePath);
            } catch (IOException e) {
                throw new InvalidExecutionStateException("Failed to read events from input file:" + inputFilePath);
            }
        } else {
            PrintInfo.info("Event Input file is not provided or file is empty");
        }

        if (!siddhiApp.equals("")) {
            if (!isDebugEnabled) {
                try {
                    SiddhiRun siddhiRun = new SiddhiRun();
                    siddhiRun.runSiddhi(siddhiApp, inputFile);
                } catch (InterruptedException e) {
                    throw new InvalidExecutionStateException("Siddhi App execution error:  " + e.getMessage());
                }
            } else {
                VMDebugManager vmDebugManager = VMDebugManager.getInstance();
                vmDebugManager.mainInit(siddhiAppPath, siddhiApp, inputFile);
            }
        } else {
            throw new InvalidExecutionStateException("No valid SiddhiApp found in the file");
        }
    }

    /**
     * Validates the Siddhi App path.
     *
     * @param siddhiAppPath path to the siddhiApp file
     */
    private static boolean validateSiddhiAppPath(String siddhiAppPath) {
        File siddhiAppFile = new File(siddhiAppPath);
        String fileName = siddhiAppFile.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (!siddhiAppFile.exists() || !siddhiAppFile.isFile()) {
            throw new InvalidExecutionStateException("Invalid siddhi app file path:" + siddhiAppPath);
        }
        if (!extension.equalsIgnoreCase("siddhi")) {
            throw new InvalidExecutionStateException("Invalid siddhi app extension detected in the file:" + extension);
        }
        return true;
    }

    /**
     * Validates the Siddhi App.
     *
     * @param siddhiAppPath path to the siddhiApp file
     */
    private static String validateAndGetSiddhdiApp(String siddhiAppPath) throws FileReadException {
        SiddhiManager siddhiManager = new SiddhiManager();
        String siddhiApp = "";
        boolean isValidSiddhiAppPath = validateSiddhiAppPath(siddhiAppPath);
        if (isValidSiddhiAppPath) {
            try {
                siddhiApp = readText(siddhiAppPath);
                SiddhiAppRuntime siddhiAppRuntime = null;
                try {
                    siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);
                    siddhiAppRuntime.start();
                } finally {
                    if (siddhiAppRuntime != null) {
                        siddhiAppRuntime.shutdown();
                        siddhiManager.shutdown();
                    }
                }
            } catch (IOException e) {
                throw new InvalidExecutionStateException("Failed to read siddhi app file:" + siddhiAppPath);
            }
        }
        return siddhiApp;
    }

    /**
     * Read the text content of the given file.
     *
     * @param path the path of the file
     * @return the text content of the file
     * @throws IOException if there are any issues in reading the file
     */
    private static String readText(String path) throws IOException, FileReadException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (FileNotFoundException e) {
            throw new FileReadException("The file " + path + " does not exist: " + e.getMessage());
        } catch (IOException e) {
            throw new FileReadException("Error in reading the file " + path + ": " + e.getMessage());
        }
    }

    public static SLauncherException createUsageException(String errorMsg) {
        SLauncherException launcherException = new SLauncherException();
        launcherException.addMessage("siddhi: " + errorMsg);
        return launcherException;
    }
}
