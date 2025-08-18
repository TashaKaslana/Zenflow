package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.registry.TransformerRegistry;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransformerRegistryTest {

    private TransformerRegistry registry;
    private DataTransformer mockTransformer1;

    @BeforeEach
    void setUp() {
        mockTransformer1 = mock(DataTransformer.class);
        DataTransformer mockTransformer2 = mock(DataTransformer.class);
        DataTransformer mockTransformer3 = mock(DataTransformer.class);

        when(mockTransformer1.getName()).thenReturn("uppercase");
        when(mockTransformer2.getName()).thenReturn("lowercase");
        when(mockTransformer3.getName()).thenReturn("trim");

        List<DataTransformer> transformers = Arrays.asList(
            mockTransformer1,
                mockTransformer2,
                mockTransformer3
        );

        registry = new TransformerRegistry(transformers);
    }

    @Test
    void testGetTransformerSuccess() {
        DataTransformer transformer = registry.getTransformer("uppercase");
        assertEquals(mockTransformer1, transformer);
    }

    @Test
    void testGetTransformerNotFound() {
        assertThrows(ExecutorException.class, () -> registry.getTransformer("nonexistent"));
    }

    @Test
    void testGetTransformerWithNullName() {
        assertThrows(ExecutorException.class, () -> registry.getTransformer(null));
    }

    @Test
    void testGetTransformerWithEmptyName() {
        assertThrows(ExecutorException.class, () -> registry.getTransformer(""));
    }

    @Test
    void testGetAllRegisteredTransformers() {
        // Test that all transformers are properly registered
        assertDoesNotThrow(() -> registry.getTransformer("uppercase"));
        assertDoesNotThrow(() -> registry.getTransformer("lowercase"));
        assertDoesNotThrow(() -> registry.getTransformer("trim"));
    }

    @Test
    void testRegistryWithEmptyList() {
        TransformerRegistry emptyRegistry = new TransformerRegistry(List.of());

        assertThrows(ExecutorException.class, () -> emptyRegistry.getTransformer("any"));
    }

    @Test
    void testRegistryWithDuplicateNames() {
        DataTransformer mockDuplicate = mock(DataTransformer.class);
        when(mockDuplicate.getName()).thenReturn("uppercase"); // Same as mockTransformer1

        List<DataTransformer> transformersWithDuplicate = Arrays.asList(
            mockTransformer1,
            mockDuplicate
        );

        // The registry should handle duplicates by using the last one in the list
        // This might throw an exception in actual implementation, so let's test that behavior
        assertThrows(IllegalStateException.class, () -> new TransformerRegistry(transformersWithDuplicate));
    }

    @Test
    void testErrorMessageContainsTransformerName() {
        String unknownTransformer = "unknown_transformer";

        ExecutorException exception = assertThrows(ExecutorException.class, () -> registry.getTransformer(unknownTransformer));

        assertTrue(exception.getMessage().contains(unknownTransformer));
        assertTrue(exception.getMessage().contains("Unknown executor type"));
    }
}
