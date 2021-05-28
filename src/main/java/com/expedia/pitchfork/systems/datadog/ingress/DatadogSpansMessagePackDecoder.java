package com.expedia.pitchfork.systems.datadog.ingress;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.expedia.pitchfork.systems.datadog.DatadogDomainConverter;
import com.expedia.pitchfork.systems.datadog.model.DatadogSpan;
import com.expedia.pitchfork.systems.zipkin.forwarder.ZipkinForwarder;
import com.expedia.pitchfork.systems.common.IngressDecoder;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zipkin2.Span;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Component
public class DatadogSpansMessagePackDecoder implements IngressDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ZipkinForwarder.class);
    private ObjectReader reader;

    @PostConstruct
    public void initialize() {
        // Instantiate ObjectMapper for MessagePack
        ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
        this.reader = mapper.readerFor(new TypeReference<List<List<DatadogSpan>>>() {
        });
    }

    @Override
    public List<Span> decodeList(byte[] bytes) {
        try {
            List<List<DatadogSpan>> traces = reader.readValue(bytes);

            return traces.stream()
                    .flatMap(Collection::stream)
                    .map(DatadogDomainConverter::toZipkin)
                    .collect(toList());
        } catch (Exception e) {
            logger.error("operation=readList", e);
            return emptyList();
        }
    }
}
