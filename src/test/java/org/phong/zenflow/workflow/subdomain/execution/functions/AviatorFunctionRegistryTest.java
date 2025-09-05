package org.phong.zenflow.workflow.subdomain.execution.functions;

import com.googlecode.aviator.Expression;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TemplateService.class,
        AviatorFunctionRegistry.class,
        StringContainsFunction.class,
        TestBooleanFunction.class
})
class AviatorFunctionRegistryTest {

    @Autowired
    private TemplateService templateService;

    @Test
    void autoRegistersBooleanFunctions() {
        Expression exp = templateService.getEvaluator().compile("fn:Test.alwaysTrue()", true);
        Object result = exp.execute(Map.of());
        assertThat(result).isEqualTo(true);
    }
}
