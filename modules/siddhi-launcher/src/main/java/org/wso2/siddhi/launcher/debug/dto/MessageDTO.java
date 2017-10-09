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

package org.wso2.siddhi.launcher.debug.dto;


import java.util.Map;

/**
 * DTO class representing the messages sent to client from the debugger.
 *
 */
public class MessageDTO {

    private String code;

    private String message;

    private String queryName;

    private Object eventInfo;

    private Map<String, Object> queryState;

    private BreakPointDTO location;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getQueryState() {
        return queryState;
    }

    public void setQueryState(Map<String, Object> queryState) {
        this.queryState = queryState;
    }

    public BreakPointDTO getLocation() {
        return location;
    }

    public void setLocation(String fileName, int queryIndex, String queryTerminal) {
        this.location = new BreakPointDTO(fileName,queryIndex,queryTerminal);
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public Object getEventInfo() {
        return eventInfo;
    }

    public void setEventInfo(Object eventInfo) {
        this.eventInfo = eventInfo;
    }

}
