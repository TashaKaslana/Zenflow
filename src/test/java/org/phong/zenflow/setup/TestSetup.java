package org.phong.zenflow.setup;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Disabled;
import org.phong.zenflow.setup.workflow.UpsertNodeSetup;
import org.phong.zenflow.setup.workflow.WorkflowRunSetup;
import org.phong.zenflow.setup.workflow.WorkflowSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("Requires containerized services")
@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSetup extends AbstractIntegrationTest {
    @Autowired
    private UserSetupTest userService;

    @Autowired
    private ProjectSetupTest projectService;

    @Autowired
    private WorkflowSetup workflowService;

    @Autowired
    private UpsertNodeSetup upsertNodeService;

    @Autowired
    private WorkflowRunSetup workflowRunSetup;

    @Autowired
    private RoleSetup roleSetup;

    @BeforeAll
    public void setup() {
        try {
            // Clear any existing context to ensure clean state
            ContextSetupHolder.clear();

            // Sequential setup with proper error handling
            roleSetup.setupRoles();
            userService.setupUsers();
            projectService.setupProjects();
            workflowService.setupWorkflows();
            upsertNodeService.setupWorkflowDefinitions();
            workflowRunSetup.startWorkflows();
        } catch (Exception e) {
            // Clean up on failure to prevent state pollution
            ContextSetupHolder.clear();
            throw new RuntimeException("Test setup failed", e);
        }
    }

    @Test
    public void testSetupCompleted() {
        // Verify that all setup completed successfully
        assert ContextSetupHolder.get("users") != null : "Users should be set up";
        assert ContextSetupHolder.get("projects") != null : "Projects should be set up";
        assert ContextSetupHolder.get("workflows") != null : "Workflows should be set up";

        System.out.println("✅ Test setup completed successfully!");
        System.out.println("Users: " + ContextSetupHolder.get("users"));
        System.out.println("Projects: " + ContextSetupHolder.get("projects"));
        System.out.println("Workflows: " + ContextSetupHolder.get("workflows"));
        System.out.println("UpsertNode: " + ContextSetupHolder.get("upsertedNodeCount"));
    }
}
