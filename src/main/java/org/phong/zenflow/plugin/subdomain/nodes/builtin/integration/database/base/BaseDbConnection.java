package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;


@AllArgsConstructor
@Slf4j
@Component
public class BaseDbConnection {

    public ResolvedDbConfig establishConnection(ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        logPublisher.info("Executing DB node with config: {}", context.getCurrentConfig());

        ResolvedDbConfig dbConfig = ResolvedDbConfig.fromInput(context);
        String connectionId = dbConfig.getConnectionIdOrGenerate();

        logPublisher.info("Creating new DataSource for connectionId: {}", connectionId);
        dbConfig.setDataSource(context.getResource(HikariDataSource.class));
        logPublisher.info("Using DataSource for connectionId: {}", connectionId);

        return dbConfig;
    }
}

