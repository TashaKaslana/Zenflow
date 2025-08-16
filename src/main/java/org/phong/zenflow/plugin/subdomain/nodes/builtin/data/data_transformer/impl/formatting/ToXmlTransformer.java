package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToXmlTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "to_xml";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object transform(Object data, Map<String, Object> params) {
        String rootElement = (String) (params != null ? params.getOrDefault("rootElement", "root") : "root");
        String itemElement = (String) (params != null ? params.getOrDefault("itemElement", "item") : "item");
        boolean pretty = (Boolean) (params != null ? params.getOrDefault("pretty", true) : true);
        String xmlDeclaration = (String) (params != null ? params.getOrDefault("xmlDeclaration", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>") : "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        StringBuilder xml = new StringBuilder();

        if (xmlDeclaration != null && !xmlDeclaration.isEmpty()) {
            xml.append(xmlDeclaration);
            if (pretty) xml.append("\n");
        }

        xml.append("<").append(rootElement).append(">");
        if (pretty) xml.append("\n");

        if (data instanceof List<?> list) {
            for (Object item : list) {
                appendElement(xml, itemElement, item, pretty, 1);
            }
        } else if (data instanceof Map<?, ?> map) {
            appendMapElements(xml, (Map<String, Object>) map, pretty, 1);
        } else {
            appendElement(xml, itemElement, data, pretty, 1);
        }

        xml.append("</").append(rootElement).append(">");

        return xml.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendElement(StringBuilder xml, String elementName, Object value, boolean pretty, int depth) {
        String indent = pretty ? "  ".repeat(depth) : "";
        String newline = pretty ? "\n" : "";

        if (value instanceof Map<?, ?> map) {
            xml.append(indent).append("<").append(elementName).append(">").append(newline);
            appendMapElements(xml, (Map<String, Object>) map, pretty, depth + 1);
            xml.append(indent).append("</").append(elementName).append(">").append(newline);
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                appendElement(xml, elementName, item, pretty, depth);
            }
        } else {
            String escapedValue = escapeXml(String.valueOf(value));
            xml.append(indent).append("<").append(elementName).append(">")
               .append(escapedValue)
               .append("</").append(elementName).append(">").append(newline);
        }
    }

    private void appendMapElements(StringBuilder xml, Map<String, Object> map, boolean pretty, int depth) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = sanitizeElementName(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                appendElement(xml, key, value, pretty, depth);
            } else {
                String indent = pretty ? "  ".repeat(depth) : "";
                String newline = pretty ? "\n" : "";
                String escapedValue = escapeXml(String.valueOf(value));
                xml.append(indent).append("<").append(key).append(">")
                   .append(escapedValue)
                   .append("</").append(key).append(">").append(newline);
            }
        }
    }

    private String sanitizeElementName(String name) {
        // Replace invalid XML element name characters
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                  .replaceAll("^[^a-zA-Z_]", "_"); // Ensure starts with letter or underscore
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
