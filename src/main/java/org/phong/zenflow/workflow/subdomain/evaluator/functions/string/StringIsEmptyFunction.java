package org.phong.zenflow.workflow.subdomain.evaluator.functions.string;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AviatorFunction
public class StringIsEmptyFunction extends AbstractFunction {
    @Override
    public String getName() {
        return "String.isEmpty";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        String text = FunctionUtils.getStringValue(arg1, env);
        boolean result = text == null || text.isEmpty();
        return AviatorRuntimeJavaType.valueOf(result);
    }
}
