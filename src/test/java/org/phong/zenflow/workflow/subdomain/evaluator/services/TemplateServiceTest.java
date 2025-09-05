package org.phong.zenflow.workflow.subdomain.evaluator.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;

class TemplateServiceTest {

    private final TemplateService templateService =
            new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction())));

    @Test
    void stringContainsFunctionWorks() {
        ExecutionContext ctx = TestExecutionContextUtils.createExecutionContext();
        Object result = templateService.resolve("{{ fn:String.contains('zenflow','flow') }}", ctx);
        assertThat(result).isEqualTo(true);
    }
}

