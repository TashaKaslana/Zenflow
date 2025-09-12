package org.phong.zenflow.secret.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.project.service.ProjectService;
import org.phong.zenflow.secret.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.infrastructure.mapstruct.SecretMapper;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretProfile;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.user.service.UserService;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.plugin.services.PluginService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecretService Tests")
class SecretServiceTest {

    @Mock
    private SecretRepository secretRepository;
    @Mock
    private SecretProfileRepository secretProfileRepository;
    @Mock
    private SecretMapper secretMapper;
    @Mock
    private AESUtil aesUtil;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private ProjectService projectService;
    @Mock
    private AuthService authService;
    @Mock
    private SecretProfileSchemaValidator validator;
    @Mock
    private PluginNodeService pluginNodeService;
    @Mock
    private PluginService pluginService;

    @InjectMocks
    private SecretService secretService;

    private UUID workflowId;
    private SecretProfile profile1;
    private SecretProfile profile2;
    private Secret secret1;
    private Secret secret2;
    private Secret secret3;

    @BeforeEach
    void setUp() throws Exception {
        workflowId = UUID.randomUUID();

        profile1 = new SecretProfile();
        profile1.setName("profileA");

        profile2 = new SecretProfile();
        profile2.setName("profileB");

        secret1 = new Secret();
        secret1.setKey("key1");
        secret1.setEncryptedValue("encryptedValue1");
        secret1.setProfile(profile1);

        secret2 = new Secret();
        secret2.setKey("key2");
        secret2.setEncryptedValue("encryptedValue2");
        secret2.setProfile(profile1);

        secret3 = new Secret();
        secret3.setKey("key3");
        secret3.setEncryptedValue("encryptedValue3");
        secret3.setProfile(profile2);

        when(aesUtil.decrypt("encryptedValue1")).thenReturn("decryptedValue1");
        when(aesUtil.decrypt("encryptedValue2")).thenReturn("decryptedValue2");
        when(aesUtil.decrypt("encryptedValue3")).thenReturn("decryptedValue3");
    }

    @Test
    @DisplayName("Should retrieve secrets grouped by profile name for a workflow")
    void shouldGetProfileSecretMapByWorkflowId() {
        // Arrange
        List<Secret> secrets = Arrays.asList(secret1, secret2, secret3);
        when(secretRepository.findByWorkflowId(workflowId)).thenReturn(secrets);

        // Act
        ProfileSecretListDto result = secretService.getProfileSecretMapByWorkflowId(workflowId);

        // Assert
        assertNotNull(result);
        Map<String, Map<String, String>> profileMap = result.profiles();
        assertNotNull(profileMap);
        assertEquals(2, profileMap.size(), "Should contain two profiles");
        assertTrue(profileMap.containsKey("profileA"));
        assertTrue(profileMap.containsKey("profileB"));

        Map<String, String> profileASecrets = profileMap.get("profileA");
        assertNotNull(profileASecrets);
        assertEquals(2, profileASecrets.size(), "ProfileA should have two secrets");
        assertEquals("decryptedValue1", profileASecrets.get("key1"));
        assertEquals("decryptedValue2", profileASecrets.get("key2"));

        Map<String, String> profileBSecrets = profileMap.get("profileB");
        assertNotNull(profileBSecrets);
        assertEquals(1, profileBSecrets.size(), "ProfileB should have one secret");
        assertEquals("decryptedValue3", profileBSecrets.get("key3"));
    }
}
