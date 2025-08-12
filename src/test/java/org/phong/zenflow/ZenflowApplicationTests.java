package org.phong.zenflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Context load test disabled in isolated CI environment")
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "SPRING_DATASOURCE=jdbc:h2:mem:testdb",
        "MAIL_HOST=localhost",
        "MAIL_PORT=25",
        "MAIL_USERNAME=user",
        "MAIL_PASSWORD=pass",
        "MAIL_SMTP_AUTH=false",
        "MAIL_SMTP_STARTTLS=false"
})
@ActiveProfiles("test")
class ZenflowApplicationTests {

	@Test
	void contextLoads() {
	}

}
