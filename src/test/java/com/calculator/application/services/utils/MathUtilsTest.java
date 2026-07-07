package com.calculator.application.services.utils;

import org.junit.jupiter.api.Test;

import static com.calculator.application.services.utils.MathUtils.round2;
import static com.calculator.application.services.utils.MathUtils.round4;

public class MathUtilsTest {

    @Test
    void testMath() {
        assert(round2(5.555) == 5.56);
        assert(round4(0.12344) == 0.1234);
    }

}
