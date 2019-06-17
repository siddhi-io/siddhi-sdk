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

package io.siddhi.sdk.launcher.debug.internal;

import io.siddhi.core.event.Event;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.sdk.launcher.debug.VMDebugSession;
import io.siddhi.sdk.launcher.exception.NoSuchStreamException;
import io.siddhi.sdk.launcher.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class which provides necessary apis for event stream related operations.
 */
public class DebuggerEventStreamService implements EventStreamService {

    private static Logger log = LoggerFactory.getLogger(DebuggerEventStreamService.class);

    private VMDebugSession debugSession;

    public VMDebugSession getDebugSession() {
        return debugSession;
    }

    public void setDebugSession(VMDebugSession debugSession) {
        this.debugSession = debugSession;
    }

    @Override
    public List<String> getStreamNames(String siddhiAppName) {
        DebugRuntime runtimeHolder = debugSession.getDebugRuntime();
        if (runtimeHolder != null) {
            return runtimeHolder.getStreams();
        } else {
            log.error("Siddhi App with name : " + siddhiAppName + " is not available");
        }
        return null;
    }

    @Override
    public List<Attribute> getStreamAttributes(String siddhiAppName, String streamName) throws
            ResourceNotFoundException {
        DebugRuntime runtimeHolder = debugSession.getDebugRuntime();
        if (runtimeHolder != null) {
            try {
                return runtimeHolder.getStreamAttributes(streamName);
            } catch (NoSuchStreamException e) {
                throw new ResourceNotFoundException("Siddhi App '" + siddhiAppName + "' does not contain " +
                        "stream '" + streamName + "'.", ResourceNotFoundException.ResourceType.STREAM_NAME, streamName);
            }
        } else {
            throw new ResourceNotFoundException("Siddhi App '" + siddhiAppName + "' does not exist.",
                    ResourceNotFoundException.ResourceType.SIDDHI_APP_NAME, siddhiAppName);
        }
    }

    @Override
    public void pushEvent(String siddhiAppName, String streamName, Event event) {
        DebugRuntime runtimeHolder = debugSession.getDebugRuntime();
        if (runtimeHolder != null) {
            try {
                runtimeHolder.getInputHandler(streamName).send(event);
            } catch (Exception e) {
                log.error("Error when pushing events to Siddhi debugger engine ", e);
            }
        }
    }
}
