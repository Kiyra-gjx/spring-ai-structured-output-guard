# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

Capa defensiva de salida estructurada para Spring AI.

`spring-ai-structured-output-guard` envuelve las llamadas de salida estructurada de Spring AI con reintentos dirigidos, reparación ligera de JSON y un Spring Boot Starter pequeño y reutilizable.

## El problema

`BeanOutputConverter` de Spring AI funciona muy bien cuando el modelo devuelve JSON válido.

En producción, es común recibir respuestas como:

````text
```json
{
  "name": "Alice",
  "skills": ["Java", "Spring",],
}
```
````

o:

```text
Here is the result you asked for:
{"name":"Alice","summary":"line1
line2"}
```

Está “casi” bien, pero no lo suficiente para un parser.

## Qué hace esta librería

- reintenta solo cuando el error parece ser de parseo estructurado
- elimina code fences y texto extra antes o después del JSON
- extrae el cuerpo JSON desde respuestas ruidosas
- elimina comas finales
- normaliza comillas tipográficas
- escapa caracteres de control dentro de cadenas JSON
- mantiene simple el código del lado del servicio
- ofrece auto-configuración para Spring Boot

## Instalación

### Gradle

```groovy
implementation "io.github.kiyragjx:spring-ai-structured-output-guard-starter:0.1.0-SNAPSHOT"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyragjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Inicio rápido

```java
return outputGuard.call(
    chatClient,
    systemPrompt,
    userPrompt,
    MovieReview.class,
    StructuredOutputCallOptions.builder()
        .logContext("movie-review")
        .failureMessage("Failed to parse movie review")
        .build()
);
```

## Configuración

```yaml
spring:
  ai:
    structured-output:
      guard:
        max-attempts: 2
        include-last-error-in-retry-prompt: true
        enable-repair: true
        max-error-message-length: 200
```

## Estrategia de reparación actual

- elimina UTF-8 BOM
- elimina Markdown code fences
- extrae el primer objeto o array JSON
- normaliza comillas inteligentes
- elimina comas finales antes de `}` o `]`
- escapa `\n`, `\r`, `\t` y otros caracteres de control dentro de cadenas JSON

## Aplicación de ejemplo

El módulo `example` expone:

```text
GET /demo/movie-review?movie=Interstellar
```

Ejecución:

```bash
./gradlew :example:bootRun
```

## Desarrollo local

```bash
./gradlew test
./gradlew :example:bootRun
```

## Compatibilidad

La base actual del proyecto usa:

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

Antes de una publicación pública, conviene migrar a una línea estable de Spring AI.

## Roadmap

- estrategias de reparación personalizadas
- métricas con Micrometer
- soporte para agregación de streaming
- más pruebas de integración

## Contribuciones

Se aceptan issues y pull requests. Consulta [CONTRIBUTING.md](./CONTRIBUTING.md).

