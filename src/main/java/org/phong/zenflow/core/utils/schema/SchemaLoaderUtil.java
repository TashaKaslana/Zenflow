package org.phong.zenflow.core.utils.schema;

import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SchemaLoaderUtil {

    public static JSONObject loadBuiltinSchema(String schemaName) {
        String filePath = "/builtin_schemas/" + schemaName + ".json";

        try (InputStream input = SchemaLoaderUtil.class.getResourceAsStream(filePath)) {
            assert input != null;
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {

                String content = scanner.useDelimiter("\\A").next();
                return new JSONObject(content);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema file: " + filePath, e);
        }
    }
}
