package com.hotels.service.tracing.zipkintohaystack.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Utility methods for tests.
 */
public class TestHelpers {

    public static byte[] compress(byte[] body) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream zipStream = new GZIPOutputStream(byteStream)) {
                zipStream.write(body);
            }
            return byteStream.toByteArray();
        }
    }
}
