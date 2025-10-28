package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class EmailExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        logCollector.info("Starting email executor");
        JavaMailSender mailSender = context.getResource();

        String to = context.read("to", String.class);
        String subject = context.read("subject", String.class);
        String body = context.read("body", String.class);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);

        logCollector.info("Sending email to: {}", to);
        mailSender.send(mailMessage);
        logCollector.success("Email sent successfully to: {}", to);

        context.write("to", to);
        context.write("subject", subject);

        return ExecutionResult.success();
    }
}
