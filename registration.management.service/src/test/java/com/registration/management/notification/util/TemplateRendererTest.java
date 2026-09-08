package com.registration.management.notification.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private TemplateRenderer templateRenderer;

    @BeforeEach
    void setUp() {
        templateRenderer = new TemplateRenderer();
    }

    @Test
    @DisplayName("Valid variables should pass validation")
    void testValidateTemplateSuccess() {
        String template = "Hello {{recipientName}}, welcome to {{eventName}} from {{schoolName}}!";
        templateRenderer.validateTemplate(template);
    }

    @Test
    @DisplayName("Invalid variables should throw IllegalArgumentException")
    void testValidateTemplateInvalidVariable() {
        String template = "Hello {{recipientName}}, your secret code is {{unknownVariable}}";
        assertThatThrownBy(() -> templateRenderer.validateTemplate(template))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknownVariable");
    }

    @Test
    @DisplayName("Render template with substituted variables")
    void testRenderTemplate() {
        String template = "Dear {{recipientName}}, your registration for {{eventName}} is approved.";
        Map<String, Object> vars = Map.of(
                "recipientName", "Alice Johnson",
                "eventName", "Science Fair 2026"
        );

        String rendered = templateRenderer.render(template, vars);
        assertThat(rendered).isEqualTo("Dear Alice Johnson, your registration for Science Fair 2026 is approved.");
    }
}
