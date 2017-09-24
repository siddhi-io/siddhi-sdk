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
package org.wso2.siddhi.launcher.exception;

import java.util.Locale;

/**
 * ResourceNotFoundException is used when a resource required is not found
 */
public class ResourceNotFoundException extends Exception {
    /**
     * ResourceType specifies types of resources
     * */
    public enum ResourceType {
        SIDDHI_APP_NAME, STREAM_NAME
    }

    private String resourceName;
    private ResourceType resourceType;

    public ResourceNotFoundException(String message, ResourceType resourceType, String resourceName) {
        super(message);
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public ResourceNotFoundException(String message,  ResourceType resourceType, String resourceName,
                                     Throwable cause) {
        super(message, cause);
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getResourceName() {
        return resourceName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceTypeString() {
        return resourceType.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
    }
}
