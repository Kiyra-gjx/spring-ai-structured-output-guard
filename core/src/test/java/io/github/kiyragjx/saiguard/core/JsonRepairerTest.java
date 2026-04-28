package io.github.kiyragjx.saiguard.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRepairerTest {

    private final JsonRepairer repairer = new JsonRepairer();

    @Test
    void shouldStripMarkdownFenceAndExtractJsonBody() {
        String raw = """
            ```json
            {"name":"guard"}
            ```
            """;

        assertEquals("{\"name\":\"guard\"}", repairer.repair(raw));
    }

    @Test
    void shouldRemoveTrailingCommas() {
        String raw = """
            {
              "name": "guard",
              "tags": ["json", "spring",],
            }
            """;

        assertEquals("{\n  \"name\": \"guard\",\n  \"tags\": [\"json\", \"spring\"]\n}", repairer.repair(raw));
    }

    @Test
    void shouldEscapeLiteralNewlinesInsideJsonStrings() {
        String raw = """
            {"message":"line1
            line2"}
            """;

        assertEquals("{\"message\":\"line1\\nline2\"}", repairer.repair(raw));
    }

    @Test
    void shouldExtractJsonBodyFromLeadingText() {
        String raw = """
            Here is the result you asked for:
            {"name":"guard"}
            Thanks.
            """;

        assertEquals("{\"name\":\"guard\"}", repairer.repair(raw));
    }

    @Test
    void shouldNormalizeSmartQuotes() {
        String raw = "{\u201Cname\u201D:\u201Cguard\u201D}";

        assertEquals("{\"name\":\"guard\"}", repairer.repair(raw));
    }

    @Test
    void shouldLeaveSingleQuotedPseudoJsonUnchanged() {
        String raw = "{'name':'guard'}";

        assertEquals(raw, repairer.repair(raw));
    }

    @Test
    void shouldLeaveTruncatedJsonUnchanged() {
        String raw = "{\"name\":\"guard\"";

        assertEquals(raw, repairer.repair(raw));
    }

    @Test
    void shouldApplyCustomStepsAfterDefaultSteps() {
        List<JsonRepairStep> steps = new ArrayList<>(JsonRepairer.defaultSteps());
        steps.add(JsonRepairStep.named("mark-guard", text -> text.replace("\"guard\"", "\"[[guard]]\"")));
        steps.add(JsonRepairStep.named("finalize-guard", text -> text.replace("[[guard]]", "patched")));

        JsonRepairer customRepairer = new JsonRepairer(steps);
        String raw = """
            ```json
            {"name":"guard",}
            ```
            """;

        assertEquals("{\"name\":\"patched\"}", customRepairer.repair(raw));
    }

    @Test
    void shouldFailFastWhenCustomStepReturnsNull() {
        JsonRepairer customRepairer = new JsonRepairer(List.of(
            JsonRepairStep.named("drop-everything", text -> null)
        ));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> customRepairer.repair("{\"name\":\"guard\"}"));

        assertEquals("Json repair step 'drop-everything' returned null", error.getMessage());
    }

    @Test
    void shouldFailFastWhenCustomStepThrows() {
        JsonRepairer customRepairer = new JsonRepairer(List.of(
            JsonRepairStep.named("explode", text -> {
                throw new IllegalArgumentException("bad repair");
            })
        ));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> customRepairer.repair("{\"name\":\"guard\"}"));

        assertEquals("Json repair step 'explode' failed", error.getMessage());
    }
}
