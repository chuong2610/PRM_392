package com.wayflo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayFloApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(WayFloApplication.class.getPackageName()).isEqualTo("com.wayflo");
    }
}
