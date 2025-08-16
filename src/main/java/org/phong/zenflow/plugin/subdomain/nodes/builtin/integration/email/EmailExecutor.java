package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@AllArgsConstructor
public class EmailExecutor implements PluginNodeExecutor {
    private final static int DEFAULT_PORT = 587;

    @Override
    public String key() {
        return "core:email:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            log.info("Starting email executor");
            logCollector.info("Executing email node with config: " + config);
            JavaMailSender mailSender = getJavaMailSender(config);

            String to = (String) config.input().get("to");
            String subject = (String) config.input().get("subject");
            String body = (String) config.input().get("body");

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);

            logCollector.info("Sending email to: " + to);
            mailSender.send(mailMessage);
            logCollector.info("Email sent successfully to: " + to);

            Map<String, Object> output = new HashMap<>();
            output.put("to", to);
            output.put("subject", subject);

            return ExecutionResult.success(output, logCollector.getLogs());
        } catch (Exception e) {
            logCollector.error("Error executing email node: " + e.getMessage(), e);
            return ExecutionResult.error(e.getMessage(), logCollector.getLogs());
        }
    }

    public JavaMailSender getJavaMailSender(WorkflowConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        String host = (String) config.input().get("host");
        Integer port = (Integer) config.input().get("port");
        String username = (String) config.input().get("username");
        String password = (String) config.input().get("password");

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
}
