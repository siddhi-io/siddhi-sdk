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

package org.wso2.siddhi.sdk.launcher.run;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.sdk.launcher.util.InputFeeder;

/**
 * Handles the run mode for a siddhi app.
 */
public class SiddhiRun {

    private static final Logger log = Logger.getLogger(SiddhiRun.class);

    // Creating Siddhi Manager
    private static SiddhiManager siddhiManager = new SiddhiManager();

    public SiddhiRun() {

    }

    public void runSiddhi(String siddhiApp, String inputFile) throws InterruptedException {

        try {
            //Generating runtime
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

            //Starting event processing
            siddhiAppRuntime.start();

            if (!(inputFile == null || inputFile.equalsIgnoreCase(""))) {
                InputFeeder inputFeeder = new InputFeeder(siddhiAppRuntime, inputFile);
                //starting input feeder
                inputFeeder.start();
            }
        } catch (Throwable e) {
            log.error("Internal Siddhi Error Occurred: " + e);
        }

    }
}
