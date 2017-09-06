///*
// * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// *
// * WSO2 Inc. licenses this file to you under the Apache License,
// * Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//package org.wso2.siddhi.runner;
//
//import org.wso2.siddhi.runner.debug.DebugRuntime;
//import org.wso2.siddhi.runner.exception.FileReadException;
//import org.wso2.siddhi.runner.exception.InvalidArgumentException;
//import org.wso2.siddhi.runner.exception.InvalidExecutionStateException;
//import org.wso2.siddhi.runner.run.SiddhiRun;
//
//import java.io.*;
//
///**
// * This is the main class for starting siddhi and debugging siddhi
// */
//public class Launcher {
//
//    public static void main(String []args) {
//
//        //TODO: Fix the skip runtime in pom file
//
//        // Validate the number of arguments
//        if (!(args.length == 2 || args.length ==3 || args.length==4)) {//TODO:Handle the arguments
//            throw new InvalidArgumentException("Expected two or three arguments but found " + args.length + "\n. " +
//                    "Please try again with  valid arguments: run <siddhi file> or debug <siddhi file> <query list>");
//        }
//
//        String runningMode = args[0];
//        String siddhiAppPath = args[1];
//        boolean debugMode=false;
//        String inputFilePath="";
//
//        if(args[2]!=null && args[4]!=null && args[2].equalsIgnoreCase("--siddhi.debug")){
//            debugMode=true;
//            inputFilePath=args[4];
//        }else{
//            inputFilePath=" ";
//        }
//
//
//        // Validate siddhiApp
//        String siddhiApp=validateSiddhiApp(siddhiAppPath);
//
//        if(!siddhiApp.equalsIgnoreCase("")){
//            if(runningMode.equalsIgnoreCase("run") && !debugMode){
//                try {
//                    SiddhiRun siddhiRun=new SiddhiRun();
//                    siddhiRun.runSiddhi(siddhiApp);
//                } catch (InterruptedException e) {
//                    throw new InvalidExecutionStateException("Siddhi App execution error:  " + e.getMessage());
//                }
//
//            }else if(runningMode.equalsIgnoreCase("run") && debugMode){
//                DebugRuntime siddhiDebug= new DebugRuntime(siddhiApp);
//                siddhiDebug.debug();
//            }
//        }
//    }
//
//    /**
//     * Validates the Siddhi App path
//     *
//     * @param siddhiAppPath path to the siddhiApp file
//     */
//    private static boolean validateSiddhiAppPath(String siddhiAppPath){
//        File siddhiAppFile = new File(siddhiAppPath);
//        if (!siddhiAppFile.exists() || !siddhiAppFile.isFile()) {
//            throw new InvalidExecutionStateException("Invalid siddhi app file path: \" + siddhiAppPath");
//        }
//        return true;
//    }
//
//    /**
//     * Validates the Siddhi App
//     *
//     * @param siddhiAppPath path to the siddhiApp file
//     */
//    private static String validateSiddhiApp(String siddhiAppPath){
//        String siddhiApp="";
//        boolean isValidSiddhiAppPath=validateSiddhiAppPath(siddhiAppPath);
//        if(isValidSiddhiAppPath){
//            try {
//                siddhiApp = readText(siddhiAppPath);
//            } catch (IOException e) {
//                throw new InvalidExecutionStateException("Failed to read siddhi app file: \" + siddhiAppPath");
//            }
//        }
//        return siddhiApp;
//    }
//
//    /**
//     * Read the text content of the given file.
//     *
//     * @param path the path of the file
//     * @return the text content of the file
//     * @throws IOException if there are any issues in reading the file
//     */
//    private static String readText(String path) throws IOException {
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
//            String line;
//            StringBuilder builder = new StringBuilder();
//            while ((line = reader.readLine()) != null) {
//                builder.append(line).append('\n');
//            }
//            return builder.toString();
//        } catch (FileNotFoundException e) {
//            throw new FileReadException("The file " + path + " does not exist "+e.getMessage());
//        } catch (IOException e) {
//            throw new FileReadException("Error in reading the file " + path+" "+e.getMessage());
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    throw new FileReadException("Error when closing the input reader of " + path+" "+e.getMessage());
//                }
//            }
//        }
//    }
//}
