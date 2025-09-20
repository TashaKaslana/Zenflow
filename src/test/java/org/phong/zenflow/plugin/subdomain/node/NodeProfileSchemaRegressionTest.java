package org.phong.zenflow.plugin.subdomain.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NodeProfileSchemaRegressionTest {

    @Test
    void googleDriveNodeSchemaReferencesPluginProfile() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> schema = mapper.readValue(
                Paths.get("src/main/java/org/phong/zenflow/plugin/subdomain/nodes/builtin/integration/google/drive/files/update/schema.json").toFile(),
                Map.class);

        Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
        Map<?, ?> profile = (Map<?, ?>) properties.get("profile");
        assertEquals("plugin:profile", profile.get("$ref"));
    }
}
