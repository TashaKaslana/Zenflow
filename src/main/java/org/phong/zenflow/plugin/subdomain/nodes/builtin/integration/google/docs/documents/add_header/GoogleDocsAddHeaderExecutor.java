package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_header;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.CreateHeaderRequest;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
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
public class GoogleDocsAddHeaderExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);
        String text = context.read("text", String.class);
        if (text == null) {
            text = "";
        }

        Docs docs = context.getResource(Docs.class);

        Request createHeader = new Request().setCreateHeader(new CreateHeaderRequest());
        BatchUpdateDocumentResponse response = docs.documents().batchUpdate(documentId,
                new BatchUpdateDocumentRequest().setRequests(List.of(createHeader))).execute();
        String headerId = response.getReplies().getFirst().getCreateHeader().getHeaderId();

        if (!text.isEmpty()) {
            InsertTextRequest insert = new InsertTextRequest()
                    .setText(text)
                    .setLocation(new Location().setSegmentId(headerId).setIndex(0));
            docs.documents().batchUpdate(documentId,
                    new BatchUpdateDocumentRequest().setRequests(List.of(new Request().setInsertText(insert))))
                    .execute();
        }

        context.write("documentId", documentId);
        context.write("headerId", headerId);
        return ExecutionResult.success();
    }
}
