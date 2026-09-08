package com.registration.management.notification.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    public static final Set<String> ALLOWED_VARIABLES = Set.of(
            "recipientName",
            "recipientEmail",
            "eventName",
            "schoolName",
            "categoryName",
            "status",
            "referenceId",
            "referenceType",
            "actionUrl",
            "remarks",
            "date",
            "time",
            "location"
    );

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    public void validateTemplate(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!ALLOWED_VARIABLES.contains(variableName)) {
                throw new IllegalArgumentException("Invalid template variable: {{" + variableName + "}}. Supported variables are: " + ALLOWED_VARIABLES);
            }
        }
    }

    public String render(String templateText, Map<String, Object> variables) {
        if (templateText == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return templateText;
        }
        String result = templateText;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, val);
        }
        return result;
    }
}
