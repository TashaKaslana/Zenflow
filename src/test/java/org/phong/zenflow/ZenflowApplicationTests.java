package org.phong.zenflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Disabled("Context initialization requires external services")
class ZenflowApplicationTests {

    @Test
    void contextLoads() {
    }

}
