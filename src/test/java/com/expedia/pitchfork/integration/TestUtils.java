package com.expedia.pitchfork.integration;

import org.awaitility.core.ConditionFactory;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class TestUtils {

    /**
     * Convenience await statement to validate async code.
     */
    public static final ConditionFactory AWAIT = await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofSeconds(1))
            .pollDelay(Duration.ofSeconds(1));
}
