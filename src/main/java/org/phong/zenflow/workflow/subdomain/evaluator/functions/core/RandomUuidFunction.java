package org.phong.zenflow.workflow.subdomain.evaluator.functions.core;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Context-free function that returns a random UUID string.
 * Usage: {{fn:randomUUID()}}
 */
@Component
@AviatorFunction
public class RandomUuidFunction extends AbstractFunction {
    @Override
    public String getName() {
        return "randomUUID";
    }

    @Override
    public AviatorObject call(Map<String, Object> env) {
        return AviatorRuntimeJavaType.valueOf(UUID.randomUUID().toString());
    }
}

