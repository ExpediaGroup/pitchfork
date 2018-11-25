package com.hotels.service.tracing.zipkintohaystack.utils;

import static java.time.LocalTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.zip.GZIPOutputStream;

import org.junit.Assert;

/**
 * Utility methods for testing async code.
 */
public class TestHelpers {

    /**
     * Run a {@link Runnable} for a give duration. Retries every time there is an exception.
     */
    public static void retryUntilSuccess(Duration timeout, Runnable lamdba) {
        LocalTime end = now().plus(timeout);
        Throwable lastError = null;

        while (now().isBefore(end)) {
            try {
                lamdba.run();
                return;
            } catch (Throwable e) {
                lastError = e;
                sleep();
            }
        }

        Assert.fail(lastError != null ? lastError.getMessage() : "Timed out without assertion being true");
    }

    private static void sleep() {
        try {
            SECONDS.sleep(1);
        } catch (Exception e) {
            // ignored
        }
    }

    public static byte[] compress(byte[] body) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream zipStream = new GZIPOutputStream(byteStream)) {
                zipStream.write(body);
            }
            return byteStream.toByteArray();
        }
    }
}
