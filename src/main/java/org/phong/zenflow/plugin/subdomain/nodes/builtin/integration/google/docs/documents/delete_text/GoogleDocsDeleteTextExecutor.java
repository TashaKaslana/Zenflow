package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.delete_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.DeleteContentRangeRequest;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocsDeleteTextExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);
        Number start = context.read("startIndex", Number.class);
        Number end = context.read("endIndex", Number.class);

        Docs docs = context.getResource(Docs.class);

        Range range = new Range().setStartIndex(start.intValue()).setEndIndex(end.intValue());
        Request request = new Request().setDeleteContentRange(
                new DeleteContentRangeRequest().setRange(range));
        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(List.of(request));
        docs.documents().batchUpdate(documentId, body).execute();

        context.write("documentId", documentId);
        context.write("deleted", true);
        return ExecutionResult.success();
    }
}
