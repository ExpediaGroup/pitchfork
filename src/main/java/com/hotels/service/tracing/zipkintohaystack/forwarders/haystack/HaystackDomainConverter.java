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

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;

/**
 * Converter between {@code Zipkin} and {@code Haystack} domains.
 */
@Component
public class HaystackDomainConverter {

    private static final String HAYSTACK_TAG_KEY_FOR_DATACENTER = "X-HAYSTACK-INFRASTRUCTURE-PROVIDER";

    private final Logger logger = LoggerFactory.getLogger(HaystackDomainConverter.class);

    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Haystack} format.
     */
    public Span fromZipkinV2(zipkin2.Span zipkin) {
        Span.Builder builder = Span.newBuilder()
                .setTraceId(zipkin.traceId())
                .setSpanId(zipkin.id());

        doIfNotNull(zipkin.name(), builder::setOperationName);
        doIfNotNull(zipkin.timestamp(), builder::setStartTime);
        doIfNotNull(zipkin.duration(), builder::setDuration);
        doIfNotNull(zipkin.parentId(), builder::setParentSpanId);
        doIfNotNull(zipkin.localServiceName(), builder::setServiceName);

        if (!isEmpty(zipkin.tags())) {
            zipkin.tags().forEach((key, value) -> {
                List<Tag> tagStream = fromZipkinTag(key, value);
                builder.addAllTags(tagStream);
            });
        }

        getTagForKind(zipkin.kind()).ifPresent(builder::addTags);

        return builder.build();
    }

    private <T> void doIfNotNull(T nullable, Consumer<T> runnable) {
        if (nullable != null) {
            runnable.accept(nullable);
        }
    }

    private Optional<Tag> getTagForKind(zipkin2.Span.Kind kind) {
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

    private List<Tag> fromZipkinTag(String key, String value) {
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
        case "datacenter": // TODO: input name should be configurable
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
