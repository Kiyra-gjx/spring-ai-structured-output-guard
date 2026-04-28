# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**No dejes que una respuesta mal formada tire toda tu cadena de salida estructurada.**

Cuando usas Spring AI con `BeanOutputConverter` en producción, el modelo todavía puede devolver de vez en cuando JSON envuelto en Markdown, JSON con comas finales o JSON mezclado con texto explicativo. En lugar de repetir limpieza, reintentos y envoltorio de excepciones en el código de negocio, conviene bajar ese trabajo a una capa guard.

## 🚀 Qué resuelve

Cuando la salida del modelo es “casi JSON válido”, el guard primero intenta una reparación local y luego decide si tiene sentido un reintento dirigido:

- Code fences
  Elimina Markdown code fences antes del parseo.
- Ruido alrededor del payload
  Extrae el primer JSON object / array desde texto explicativo.
- Errores leves de sintaxis
  Repara comas finales, smart quotes y caracteres de control dentro de cadenas JSON.
- Fallos de parseo
  Reintenta solo cuando el error restante sigue pareciendo un problema de parseo de salida estructurada.

La comparación completa y la matriz de ejemplos malformed JSON están en [docs/adoption-notes.md](./docs/adoption-notes.md).

## 🛠️ Inicio rápido

### 1. Añade la dependencia

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. Úsalo como compañero de `ChatClient`

El guard envuelve toda la cadena “llamar al modelo -> parsear -> reparar si hace falta -> reintentar si hace falta”, así que no necesitas gestionar a mano `converter.convert()` ni clasificar excepciones en cada servicio.

```java
@Service
public class ResumeService {

    private final ChatClient chatClient;
    private final SpringAiStructuredOutputGuard outputGuard;

    public ResumeService(ChatClient.Builder chatClientBuilder,
                         SpringAiStructuredOutputGuard outputGuard) {
        this.chatClient = chatClientBuilder.build();
        this.outputGuard = outputGuard;
    }

    public ResumeSummary summarize(String resumeText) {
        return outputGuard.call(
            chatClient,
            "You are a recruiting assistant.",
            "Resume content:\n" + resumeText,
            ResumeSummary.class,
            StructuredOutputCallOptions.builder()
                .logContext("resume-task")
                .failureMessage("Failed to parse resume summary")
                .build()
        );
    }
}
```

## ⚖️ Cómo elegir

| Opción | Cuándo encaja |
|---|---|
| Native structured output | Tu proveedor lo soporta oficialmente y quieres apoyarte primero en eso |
| `BeanOutputConverter` puro | El modelo suele devolver JSON válido y no necesitas una capa de recuperación |
| `StructuredOutputValidationAdvisor` | Necesitas validación por schema y reintentos, pero la limpieza de JSON no es el problema central |
| Spring AI Guard | El problema real son code fences, comas finales, texto envolvente o caracteres de control en JSON recuperable |

Si solo quieres ver la comparación detallada, ve a [docs/adoption-notes.md](./docs/adoption-notes.md).

## ⚙️ Configuración

El prefijo de propiedades es `spring.ai.structured-output.guard`.

```yaml
spring:
  ai:
    structured-output:
      guard:
        max-attempts: 2
        enable-repair: true
        include-last-error-in-retry-prompt: true
        max-error-message-length: 200
        metrics:
          enabled: true
```

| Propiedad | Valor por defecto | Descripción |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | Número total de intentos, incluyendo la primera llamada |
| `spring.ai.structured-output.guard.enable-repair` | `true` | Activa la reparación ligera de JSON antes del reintento |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | Añade el error resumido de parseo al prompt de reintento |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | Limita la longitud del error incluido en el prompt de reintento |
| `spring.ai.structured-output.guard.metrics.enabled` | `true` | Activa el listener de Micrometer cuando existe un Bean `MeterRegistry` |

## 🔧 Estrategia de reparación

La capa de reparación es conservadora por diseño. Solo toca ruido de formato con bajo riesgo de normalización:

- elimina UTF-8 BOM
- elimina Markdown code fences
- extrae el primer JSON object / array
- normaliza smart quotes
- elimina comas finales antes de `}` o `]`
- escapa `\n`, `\r`, `\t` y otros caracteres de control dentro de cadenas JSON

No intenta:

- adivinar llaves o corchetes faltantes
- reescribir pseudo JSON con comillas simples a JSON estándar
- eliminar comentarios JSON
- corregir payloads que ya son semánticamente erróneos

Esos casos pasan a reintento dirigido o terminan en `StructuredOutputException`.

## Extender la reparación

Si el repair por defecto está cerca de lo que necesitas pero no llega del todo, ahora tienes dos vías de extensión:

- Añadir uno o más Beans `JsonRepairStep` cuando quieras conservar los pasos integrados y agregar limpieza personalizada al final.
- Proveer tu propio Bean `JsonRepairer` cuando necesites quitar, reordenar o sustituir por completo la cadena de pasos por defecto.

### Añadir pasos de repair personalizados

Los Beans `JsonRepairStep` personalizados se ejecutan después de los pasos integrados. En aplicaciones Spring se aplican según el orden de `@Order` / `Ordered`.

```java
@Configuration
class StructuredOutputRepairConfig {

    @Bean
    @Order(100)
    JsonRepairStep normalizeAngleQuoteTokens() {
        return JsonRepairStep.named("normalizeAngleQuoteTokens",
            text -> text.replace("<<", "\""));
    }
}
```

### Sustituir o ajustar toda la cadena de pasos

Si necesitas más control, define tu propio Bean `JsonRepairer` y construye exactamente la cadena que quieras. `JsonRepairer.defaultSteps()` expone la secuencia integrada actual para que puedas partir de ella y modificarla.

```java
@Configuration
class StructuredOutputRepairConfig {

    @Bean
    JsonRepairer jsonRepairer() {
        List<JsonRepairStep> steps = new ArrayList<>(JsonRepairer.defaultSteps());
        steps.removeIf(step -> step.name().equals("normalizeQuotes"));
        steps.add(JsonRepairStep.named("normalizeAngleQuoteTokens",
            text -> text.replace("<<", "\"")));
        return new JsonRepairer(steps);
    }
}
```

Cada paso recibe la salida del paso anterior. Si un paso personalizado lanza una excepción o devuelve `null`, `JsonRepairer` hace fail-fast con `IllegalStateException` en lugar de ignorarlo silenciosamente.

## Observabilidad

Si tu aplicación ya expone un `MeterRegistry` de Micrometer, el starter publica automáticamente contadores de salida estructurada. No exige un backend de monitorización concreto; cualquier registry compatible con Micrometer en Spring Boot puede recogerlos.

Si quieres conservar el comportamiento del guard pero desactivar el listener integrado de Micrometer, configura `spring.ai.structured-output.guard.metrics.enabled=false`.

| Métrica | Tags | Significado |
|---|---|---|
| `spring.ai.structured.output.guard.calls` | `result=success|repaired_success|failure` | Total de llamadas completadas del guard agrupadas por resultado final |
| `spring.ai.structured.output.guard.repair.attempts` | ninguno | Número de veces que se entra en el flujo de repair local tras fallos de parseo |
| `spring.ai.structured.output.guard.repair.success` | ninguno | Número de veces que el contenido reparado se parsea con éxito |
| `spring.ai.structured.output.guard.retries` | `error_type=structured_output|other` | Número de reintentos realmente programados |
| `spring.ai.structured.output.guard.failures` | `error_type=structured_output|other` | Número de fallos finales después del procesamiento del guard |

`repair.attempts` cuenta pasadas de repair, no solo peticiones de alto nivel. Si una misma petición falla dos veces al parsear y entra dos veces en repair antes del resultado final, el contador aumenta en `2`.

Si integras directamente `core` en lugar del starter Spring, también puedes pasar tu propio `StructuredOutputExecutionListener` a `StructuredOutputExecutor` y enviar esos mismos eventos de ciclo de vida a tu pila de observabilidad.

## 🧱 Estructura del proyecto

- `core`
  Reintentos, reparación y orquestación de parseo sin dependencia del contenedor Spring.
- `starter`
  Auto-configuración Spring Boot y punto de entrada listo para usar.
- `example`
  Demo ejecutable.

## 📌 Compatibilidad

Combinaciones verificadas actualmente en este repositorio, validadas por última vez el 28 de abril de 2026:

| Spring Boot | Spring AI | Estado | Ruta de validación |
|---|---|---|---|
| `4.0.1` | `2.0.0-M5` | Verificado | Ruta por defecto `./gradlew test`, cubriendo `core`, `starter` y `example` |
| `3.5.11` | `1.1.4` | Verificado | Ejecución de la matriz de compatibilidad con `-PspringBootVersion=3.5.11 -PspringAiVersion=1.1.4` |

Líneas públicas actuales de Spring AI, a fecha del 28 de abril de 2026:

- Stable `1.0.5` y `1.1.4`
- Preview `2.0.0-M5`

La build por defecto del repositorio apunta ahora a `Spring Boot 4.0.1 + Spring AI 2.0.0-M5`.

Otras líneas de Spring AI no están cubiertas hoy por la matriz de compatibilidad en CI y deben tratarse como no verificadas hasta que se añadan y se prueben explícitamente.

## 🤝 Contribuciones

Se aceptan issues y pull requests.

- Notas de release: [CHANGELOG.md](./CHANGELOG.md)
- Guía de contribución: [CONTRIBUTING.md](./CONTRIBUTING.md)
