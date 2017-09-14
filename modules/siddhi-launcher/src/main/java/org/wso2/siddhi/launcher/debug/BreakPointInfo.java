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
package org.wso2.siddhi.launcher.debug;

import java.util.Map;

/**
 * {@link BreakPointInfo} represents information about current BreakPoint.
 */
public class BreakPointInfo {

    private int queryIndex;

    private String queryTerminal;

    private String fileName;

    private int lineNumber;

    private Map<String, Object> queryState;

    public Integer getQueryIndex() {
        return queryIndex;
    }

    public String getQueryTerminal() {
        return queryTerminal;
    }

    public Map<String, Object> getQueryState() {
        return queryState;
    }

    public void setQueryState(Map<String, Object> queryState) {
        this.queryState = queryState;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public BreakPointInfo(String fileName, int lineNumber,int queryIndex, String queryTerminal) {
        this.fileName=fileName;
        this.lineNumber=lineNumber;
        this.queryIndex = queryIndex;
        this.queryTerminal = queryTerminal;
    }

}
