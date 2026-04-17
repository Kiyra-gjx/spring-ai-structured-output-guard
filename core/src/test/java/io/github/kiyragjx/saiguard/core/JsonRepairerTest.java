package io.github.kiyragjx.saiguard.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
