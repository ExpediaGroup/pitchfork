/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.hotels.service.tracing.zipkintohaystack;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import java.util.Map;

import static java.lang.Boolean.TRUE;

public class ForwarderPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final String FORWARDER_ENABLED_PATTERN = "^pitchfork\\.forwarders\\..+\\.enabled$";

    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        Map<String, Object> systemProperties = event.getEnvironment().getSystemProperties();
        if (systemProperties.entrySet().stream()
                .noneMatch(property -> property.getKey().matches(FORWARDER_ENABLED_PATTERN) && TRUE.toString().equals(property.getValue()))) {
            throw new IllegalStateException("No span forwarders configured. See README.md for a list of available span forwarders.");
        }
    }
}
