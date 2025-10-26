package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.append_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertTextRequest;
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
public class GoogleDocsAppendTextExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);
        String text = context.read("text", String.class);

        Docs docs = context.getResource(Docs.class);

        InsertTextRequest insert = new InsertTextRequest()
                .setText(text)
                .setEndOfSegmentLocation(new EndOfSegmentLocation());
        Request request = new Request().setInsertText(insert);
        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest()
                .setRequests(List.of(request));

        docs.documents().batchUpdate(documentId, body).execute();

        context.write("documentId", documentId);
        context.write("text", text);
        return ExecutionResult.success();
    }
}
