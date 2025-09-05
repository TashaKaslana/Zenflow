package org.phong.zenflow.workflow.subdomain.evaluator.functions.string;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorBooleanFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * String helper providing {@code fn:String.contains(text, search)} semantics.
 */
@Component
@AviatorBooleanFunction
public class StringContainsFunction extends AbstractFunction {

    @Override
    public String getName() {
        return "String.contains";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        String source = FunctionUtils.getStringValue(arg1, env);
        String search = FunctionUtils.getStringValue(arg2, env);
        boolean result = source != null && search != null && source.contains(search);
        return AviatorRuntimeJavaType.valueOf(result);
    }
}
