/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.launcher;

import com.beust.jcommander.*;
import org.wso2.siddhi.launcher.exception.SLangRuntimeException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.siddhi.launcher.Constants.SYSTEM_PROP_SIDDHI_DEBUG;

/**
 * This class executes a siddhi program.
 */
public class Main {

    private static final String JC_UNKNOWN_OPTION_PREFIX = "Unknown option:";
    private static final String JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX = "Expected a value after parameter";

    private static PrintStream outStream = System.err;

    public static void main(String... args) {

        try {
            Optional<SLauncherCmd> optionalInvokedCmd = getInvokedCmd(args);
            optionalInvokedCmd.ifPresent(SLauncherCmd::execute);
        } catch (SLangRuntimeException e) {
            outStream.println(e.getMessage());
            Runtime.getRuntime().exit(1);
        } catch (SLauncherException e) {
            LauncherUtils.printLauncherException(e, outStream);
            Runtime.getRuntime().exit(1);
        } catch (Throwable e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "siddhi: internal error occurred";
            } else {
                msg = "siddhi: " + LauncherUtils.makeFirstLetterLowerCase(msg);
            }
            outStream.println(msg);
            Runtime.getRuntime().exit(1);
        }
    }

    private static Optional<SLauncherCmd> getInvokedCmd(String... args) {
        try {

            // Run command
            RunCmd runCmd = new RunCmd();
            JCommander cmdParser = new JCommander(runCmd);
            cmdParser.setProgramName("siddhi");
            cmdParser.parse(args);
            String parsedCmdName = cmdParser.getParsedCommand();
            // User has not specified a command. Therefore returning the main command
            // which simply prints usage information.
            if (parsedCmdName == null) {
                return Optional.of(runCmd);
            }

            Map<String, JCommander> commanderMap = cmdParser.getCommands();
            return Optional.of((SLauncherCmd) commanderMap.get(parsedCmdName).getObjects().get(0));

        } catch (MissingCommandException e) {
            String errorMsg = "unknown command '" + e.getUnknownCommand() + "'";
            throw LauncherUtils.createUsageException(errorMsg);

        } catch (ParameterException e) {
            String msg = e.getMessage();
            if (msg == null) {
                throw LauncherUtils.createUsageException("internal error occurred");

            } else if (msg.startsWith(JC_UNKNOWN_OPTION_PREFIX)) {
                String flag = msg.substring(JC_UNKNOWN_OPTION_PREFIX.length());
                throw LauncherUtils.createUsageException("unknown flag '" + flag.trim() + "'");

            } else if (msg.startsWith(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX)) {
                String flag = msg.substring(JC_EXPECTED_A_VALUE_AFTER_PARAMETER_PREFIX.length());
                throw LauncherUtils.createUsageException("flag '" + flag.trim() + "' needs an argument");

            } else {
                // Make the first character of the error message lower case
                throw LauncherUtils.createUsageException(LauncherUtils.makeFirstLetterLowerCase(msg));
            }
        }
    }

    /**
     * This class represents the "run" command and it holds arguments and flags specified by the user.
     */
    @Parameters(commandNames = "run", commandDescription = "compile and run Siddhi program")
    private static class RunCmd implements SLauncherCmd{

        @Parameter(arity = 1, description = "arguments")
        private List<String> argList;

        @Parameter(names = "--siddhi.debug", hidden = true, description = "remote debugging port")
        private String siddhiDebugPort;

        @Override
        public void execute() {

            boolean debugMode=false;

            if (argList == null || argList.size() == 0) {
                throw new RuntimeException("no siddhi program given");
            }

            // Enable remote debugging
            if (siddhiDebugPort != null ) {
                System.setProperty(SYSTEM_PROP_SIDDHI_DEBUG, siddhiDebugPort);
                debugMode=true;
            }

            // Filter out the list of arguments given to the siddhi program.
            String[] programArgs;
            if (argList.size() >= 2) {
                argList.remove(0);
                programArgs = argList.toArray(new String[0]);
            } else {
                programArgs = new String[0];
            }

            LauncherUtils.runProgram(debugMode,programArgs);
        }
    }

}
