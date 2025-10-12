package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.format_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocsFormatTextExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);
        Number start = context.read("startIndex", Number.class);
        Number end = context.read("endIndex", Number.class);
        Boolean bold = context.read("bold", Boolean.class);
        if (bold == null) {
            bold = Boolean.FALSE;
        }
        Number fontSize = context.read("fontSize", Number.class);

        Docs docs = context.getResource(Docs.class);

        TextStyle style = new TextStyle().setBold(bold);
        if (fontSize != null) {
            style.setFontSize(new com.google.api.services.docs.v1.model.Dimension()
                    .setMagnitude(fontSize.doubleValue())
                    .setUnit("PT"));
        }

        UpdateTextStyleRequest styleRequest = new UpdateTextStyleRequest()
                .setRange(new Range().setStartIndex(start.intValue()).setEndIndex(end.intValue()))
                .setTextStyle(style)
                .setFields(fontSize != null ? "bold,fontSize" : "bold");

        Request request = new Request().setUpdateTextStyle(styleRequest);
        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(List.of(request));
        docs.documents().batchUpdate(documentId, body).execute();

        Map<String, Object> output = new HashMap<>();
        output.put("documentId", documentId);
        output.put("formatted", true);
        return ExecutionResult.success(output);
    }
}
