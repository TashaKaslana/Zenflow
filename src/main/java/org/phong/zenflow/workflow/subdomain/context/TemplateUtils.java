package org.phong.zenflow.workflow.subdomain.context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtils {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    
    public static List<String> extractRefs(String template) {
        List<String> refs = new ArrayList<>();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            refs.add(matcher.group(1).trim());
        }
        
        return refs;
    }
    
    public static boolean isTemplate(String value) {
        return value != null && TEMPLATE_PATTERN.matcher(value).find();
    }
}