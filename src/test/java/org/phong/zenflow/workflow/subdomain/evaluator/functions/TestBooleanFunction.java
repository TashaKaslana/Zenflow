package org.phong.zenflow.workflow.subdomain.evaluator.functions;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AviatorFunction
public class TestBooleanFunction extends AbstractFunction {
    @Override
    public String getName() {
        return "Test.alwaysTrue";
    }

    @Override
    public AviatorObject call(Map<String, Object> env) {
        return AviatorRuntimeJavaType.valueOf(true);
    }
}
