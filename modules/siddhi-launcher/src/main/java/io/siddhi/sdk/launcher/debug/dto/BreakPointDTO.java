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

package io.siddhi.sdk.launcher.debug.dto;

/**
 * Break point DTO class.
 */
public class BreakPointDTO {

    private String fileName;

    private int queryIndex;

    private String queryTerminal;

    //DO not remove this constructor. This is used in the GSON to covert data from a json string to actual class
    public BreakPointDTO() {

    }

    public BreakPointDTO(String fileName, int queryIndex, String queryTerminal) {
        this.fileName = fileName;
        this.queryIndex = queryIndex;
        this.queryTerminal = queryTerminal;
    }

    public Integer getQueryIndex() {
        return queryIndex;
    }

    public void setQueryIndex(Integer queryIndex) {
        this.queryIndex = queryIndex;
    }

    public String getQueryTerminal() {
        return queryTerminal;
    }

    public void setQueryTerminal(String queryTerminal) {
        this.queryTerminal = queryTerminal;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
