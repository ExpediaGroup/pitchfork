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
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import static java.util.Optional.empty;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;

/**
 * Converter between {@code Zipkin} and {@code Haystack} domains.
 */
public class HaystackDomainConverter {

    private static final String HAYSTACK_TAG_KEY_FOR_DATACENTER = "X-HAYSTACK-INFRASTRUCTURE-PROVIDER";

    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Haystack} format.
     */
    public static Span fromZipkinV2(zipkin2.Span zipkin) {
        Span.Builder builder = Span.newBuilder()
                .setTraceId(zipkin.traceId())
                .setSpanId(zipkin.id());

        doIfNotNull(zipkin.name(), builder::setOperationName);
        doIfNotNull(zipkin.timestamp(), builder::setStartTime);
        doIfNotNull(zipkin.duration(), builder::setDuration);
        doIfNotNull(zipkin.parentId(), builder::setParentSpanId);
        doIfNotNull(zipkin.localServiceName(), builder::setServiceName);

        List<Tag> tags = zipkin.tags().entrySet().stream()
                .flatMap(entry -> fromZipkinTag(entry.getKey(), entry.getValue()).stream())
                .collect(toList());
        builder.addAllTags(tags);

        var logs = zipkin.annotations().stream()
                .map(annotation -> Log.newBuilder()
                        .setTimestamp(annotation.timestamp())
                        .addFields(Tag.newBuilder()
                                .setKey("annotation")
                                .setVStr(annotation.value())
                                .build())
                        .build())
                .collect(toList());
        builder.addAllLogs(logs);

        getTagForKind(zipkin.kind()).ifPresent(builder::addTags);

        return builder.build();
    }

    private static <T> void doIfNotNull(T nullable, Consumer<T> runnable) {
        if (nullable != null) {
            runnable.accept(nullable);
        }
    }

    private static Optional<Tag> getTagForKind(zipkin2.Span.Kind kind) {
        String value = null;

        if (kind != null) {
            switch (kind) {
                case CLIENT:
                    value = "client";
                    break;
                case SERVER:
                    value = "server";
                    break;
                case CONSUMER:
                    value = "consumer";
                    break;
                case PRODUCER:
                    value = "producer";
                    break;
            }

            return Optional.of(Tag.newBuilder()
                    .setKey("span.kind")
                    .setVStr(value)
                    .setType(Tag.TagType.STRING)
                    .build());
        } else {
            return empty();
        }
    }

    private static List<Tag> fromZipkinTag(String key, String value) {
        switch (key) {
            case "error":
                // Zipkin error tags are Strings where as in Haystack they're Booleans
                // Since a Zipkin error tag may contain relevant information about the error we expand it into two tags (error + error message)
                Tag errorTag = Tag.newBuilder()
                        .setKey(key)
                        .setVBool(true)
                        .setType(Tag.TagType.BOOL)
                        .build();

                Tag errorMessageTag = Tag.newBuilder()
                        .setKey("error_msg")
                        .setVStr(value)
                        .setType(Tag.TagType.STRING)
                        .build();

                return List.of(errorTag, errorMessageTag);
            case "datacenter":
                Tag haystackDcTag = Tag.newBuilder()
                        .setKey(HAYSTACK_TAG_KEY_FOR_DATACENTER)
                        .setVStr(value)
                        .setType(Tag.TagType.STRING)
                        .build();

                return List.of(haystackDcTag);
            default:
                break;
        }

        Tag tag = Tag.newBuilder()
                .setKey(key)
                .setVStr(value)
                .setType(Tag.TagType.STRING)
                .build();

        return List.of(tag);
    }
}
