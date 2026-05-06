package io.github.kiyragjx.saiguard.starter;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationMetadataConsistencyTest {

    private static final Map<String, PropertyExpectation> EXPECTED_PROPERTIES = new LinkedHashMap<>();

    static {
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.max-attempts", new PropertyExpectation(
            "java.lang.Integer",
            "2",
            "Total attempts including the first call"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.enable-repair", new PropertyExpectation(
            "java.lang.Boolean",
            "true",
            "Enables lightweight JSON repair before retrying"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.include-last-error-in-retry-prompt", new PropertyExpectation(
            "java.lang.Boolean",
            "true",
            "Adds the sanitized parse error to retry instructions"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.max-error-message-length", new PropertyExpectation(
            "java.lang.Integer",
            "200",
            "Truncates parse errors included in retry prompts"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.retry-on-structured-output-error", new PropertyExpectation(
            "java.lang.Boolean",
            "true",
            "Retries errors classified as structured-output parsing failures while attempts remain"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.retry-on-other-error", new PropertyExpectation(
            "java.lang.Boolean",
            "false",
            "Retries errors that are not classified as structured-output parsing failures"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.retry-backoff-millis", new PropertyExpectation(
            "java.lang.Long",
            "0",
            "Fixed wait before each retry; 0 means no wait"
        ));
        EXPECTED_PROPERTIES.put("spring.ai.structured-output.guard.metrics.enabled", new PropertyExpectation(
            "java.lang.Boolean",
            "true",
            "Enables the Micrometer listener when a MeterRegistry bean is present"
        ));
    }

    @Test
    void generatedConfigurationMetadataShouldDescribeEveryPublicProperty() throws IOException {
        Map<String, Map<String, Object>> metadataProperties = generatedMetadataPropertiesByName();

        assertThat(metadataProperties).containsOnlyKeys(EXPECTED_PROPERTIES.keySet());

        EXPECTED_PROPERTIES.forEach((propertyName, expectation) -> {
            Map<String, Object> property = metadataProperties.get(propertyName);

            assertThat(property.get("type")).isEqualTo(expectation.type());
            assertThat(String.valueOf(property.get("defaultValue"))).isEqualTo(expectation.defaultValue());
            assertThat(normalizeDescription((String) property.get("description")))
                .isEqualTo(normalizeDescription(expectation.description()));
        });
    }

    @Test
    void readmeConfigurationTablesShouldStayInSyncWithPublicProperties() throws IOException {
        assertReadmeTableMatches("README.md", true);
        assertReadmeTableMatches("README.zh-CN.md", false);
        assertReadmeTableMatches("README.es.md", false);
        assertReadmeTableMatches("README.ja.md", false);
    }

    private Map<String, Map<String, Object>> generatedMetadataPropertiesByName() throws IOException {
        try (InputStream input = getClass().getClassLoader()
            .getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertThat(input).as("generated Spring Boot configuration metadata").isNotNull();

            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            List<Map<String, Object>> properties = JsonPath.read(json, "$.properties[*]");
            Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
            for (Map<String, Object> property : properties) {
                String name = (String) property.get("name");
                if (name.startsWith("spring.ai.structured-output.guard.")) {
                    byName.put(name, property);
                }
            }
            return byName;
        }
    }

    private void assertReadmeTableMatches(String fileName, boolean compareDescriptions) throws IOException {
        Map<String, ReadmeRow> rows = readmeConfigurationRows(fileName);

        assertThat(rows).as(fileName + " configuration rows").containsOnlyKeys(EXPECTED_PROPERTIES.keySet());

        EXPECTED_PROPERTIES.forEach((propertyName, expectation) -> {
            ReadmeRow row = rows.get(propertyName);

            assertThat(row.defaultValue()).as(fileName + " default for " + propertyName)
                .isEqualTo(expectation.defaultValue());
            if (compareDescriptions) {
                assertThat(normalizeDescription(row.description()))
                    .as(fileName + " description for " + propertyName)
                    .isEqualTo(normalizeDescription(expectation.description()));
            }
        });
    }

    private Map<String, ReadmeRow> readmeConfigurationRows(String fileName) throws IOException {
        Path readme = repositoryRoot().resolve(fileName);
        List<String> lines = Files.readAllLines(readme, StandardCharsets.UTF_8);
        Map<String, ReadmeRow> rows = new LinkedHashMap<>();

        for (String line : lines) {
            if (!line.startsWith("| `spring.ai.structured-output.guard.")) {
                continue;
            }
            String[] cells = line.split("\\|", -1);
            assertThat(cells).as(fileName + " configuration table row: " + line).hasSizeGreaterThanOrEqualTo(5);
            String propertyName = trimBackticks(cells[1]);
            String defaultValue = trimBackticks(cells[2]);
            String description = cells[3].trim();
            rows.put(propertyName, new ReadmeRow(defaultValue, description));
        }

        return rows;
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("README.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root from " + System.getProperty("user.dir"));
    }

    private String trimBackticks(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("`") && trimmed.endsWith("`")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeDescription(String value) {
        return value
            .replace("`", "")
            .replaceAll("\\.$", "")
            .trim();
    }

    private record PropertyExpectation(String type, String defaultValue, String description) {
    }

    private record ReadmeRow(String defaultValue, String description) {
    }
}
