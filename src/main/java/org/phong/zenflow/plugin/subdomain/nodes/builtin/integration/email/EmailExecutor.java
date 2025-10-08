package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class EmailExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        logCollector.info("Starting email executor");
        logCollector.info("Executing email node with config: {}", config);
        JavaMailSender mailSender = context.getResource();

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
    }
}
