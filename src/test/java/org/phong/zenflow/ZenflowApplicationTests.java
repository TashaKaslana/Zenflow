package org.phong.zenflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Disabled("Requires full application context")
@SpringBootTest
class ZenflowApplicationTests {

    @Test
    void contextLoads() {
    }

}
