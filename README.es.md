# Spring AI Structured Output Guard 🛡️

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kiyra-gjx/spring-ai-structured-output-guard-starter)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

**No dejes que una respuesta mal formada tumbe toda tu salida estructurada.**

Cuando usas Spring AI con `BeanOutputConverter` en producción, el modelo puede seguir devolviendo de vez en cuando JSON envuelto en Markdown, JSON con comas finales o JSON mezclado con texto explicativo. En lugar de repetir limpieza, reintentos y envoltorio de excepciones en el código de negocio, conviene bajar ese trabajo a una capa guard.

## 🚀 Qué resuelve

Cuando la salida del modelo es "casi JSON válido", el guard primero intenta una reparación local y luego decide si tiene sentido un reintento dirigido:

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

El guard envuelve toda la cadena "llamar al modelo -> parsear -> reparar si hace falta -> reintentar si hace falta", así que no necesitas gestionar a mano `converter.convert()` ni clasificar excepciones en cada servicio.

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
```

| Propiedad | Valor por defecto | Descripción |
|---|---:|---|
| `spring.ai.structured-output.guard.max-attempts` | `2` | Número total de intentos, incluyendo la primera llamada |
| `spring.ai.structured-output.guard.enable-repair` | `true` | Activa la reparación ligera de JSON antes del reintento |
| `spring.ai.structured-output.guard.include-last-error-in-retry-prompt` | `true` | Añade el error resumido de parseo al prompt de reintento |
| `spring.ai.structured-output.guard.max-error-message-length` | `200` | Limita la longitud del error incluido en el prompt de reintento |

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

## 🧱 Estructura del proyecto

- `core`
  Reintentos, reparación y orquestación de parseo sin dependencia del contenedor Spring.
- `starter`
  Auto-configuración Spring Boot y punto de entrada listo para usar.
- `example`
  Demo ejecutable.

## 📌 Compatibilidad

Base realmente validada en este repositorio:

- Java `21`
- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`

Líneas públicas actuales de Spring AI, a fecha del 17 de abril de 2026:

- Stable `1.0.5` y `1.1.4`
- Preview `2.0.0-M4`

`0.1.0` significa que el proyecto fue verificado sobre esa base. No debe leerse como una promesa general de compatibilidad con todas las líneas actuales de Spring AI.

## 🤝 Contribuciones

Se aceptan issues y pull requests.

- Notas de release: [CHANGELOG.md](./CHANGELOG.md)
- Guía de contribución: [CONTRIBUTING.md](./CONTRIBUTING.md)
