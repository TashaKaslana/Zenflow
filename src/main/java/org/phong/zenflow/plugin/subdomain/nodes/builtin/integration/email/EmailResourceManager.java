package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Component
public class EmailResourceManager extends BaseNodeResourceManager<JavaMailSender, ResourceConfig> {
    private final static int DEFAULT_PORT = 587;

    @Override
    public ResourceConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String host = (String) cfg.input().get("host");
        Integer port = (Integer) cfg.input().get("port");
        String username = (String) cfg.input().get("username");
        String password = (String) cfg.input().get("password");

        String key = host + ":" + port + "/" + username + "@" + Integer.toHexString(password.hashCode());
        Map<String, Object> params = Map.of(
                "host", host,
                "port", port,
                "username", username,
                "password", password
        );

        return new DefaultResourceConfig(params, key);
    }

    @Override
    protected JavaMailSender createResource(String resourceKey, ResourceConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        String host = config.getConfigValue("host", String.class);
        Integer port = config.getConfigValue("port", Integer.class);
        String username = config.getConfigValue("username", String.class);
        String password = config.getConfigValue("password", String.class);

        mailSender.setHost(host);
        mailSender.setPort(port != null ? port : DEFAULT_PORT);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    @Override
    protected void cleanupResource(JavaMailSender resource) {

    }
}
