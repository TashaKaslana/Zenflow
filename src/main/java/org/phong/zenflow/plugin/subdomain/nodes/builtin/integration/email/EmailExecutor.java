package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
@AllArgsConstructor
public class EmailExecutor implements NodeExecutor {
    private final static int DEFAULT_PORT = 587;
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            logCollector.info("Starting email executor");
            logCollector.info("Executing email node with config: {}", config);
            JavaMailSender mailSender = getJavaMailSender(config);

            String to = (String) config.input().get("to");
            String subject = (String) config.input().get("subject");
            String body = (String) config.input().get("body");

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);

            logCollector.info("Sending email to: {}", to);
            mailSender.send(mailMessage);
            logCollector.success("Email sent successfully to: {}", to);

            Map<String, Object> output = new HashMap<>();
            output.put("to", to);
            output.put("subject", subject);

            return ExecutionResult.success(output);
        } catch (Exception e) {
            logCollector.withException(e).error("Error executing email node: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
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
