# Spring AI Structured Output Guard

[English](./README.md) | [简体中文](./README.zh-CN.md) | [日本語](./README.ja.md) | [Español](./README.es.md)

[![CI](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/Kiyra-gjx/spring-ai-structured-output-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-437291?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M1-6DB33F)](https://spring.io/projects/spring-ai)
[![License: Apache--2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

Una pequeña capa de protección para las llamadas de salida estructurada de Spring AI.

`spring-ai-structured-output-guard` añade una capa pequeña de protección alrededor del flujo de salida estructurada de Spring AI. Reintenta solo cuando el fallo parece un problema de parseo y aplica una limpieza ligera de JSON antes de fallar definitivamente.

La integración externa ya fue verificada con un proyecto Maven independiente usando las coordenadas publicadas en Maven Central y una API compatible con OpenAI Chat Completions.

## Por qué existe

Spring AI ya ofrece `BeanOutputConverter`, y funciona bien cuando el modelo devuelve JSON válido.

Esta librería apunta a otro caso: respuestas que están cerca de ser JSON, pero no lo suficiente. Los problemas más comunes son:

- JSON envuelto en Markdown code fences
- comas finales antes de `}` o `]`
- texto explicativo antes o después del cuerpo JSON
- saltos de línea en bruto y caracteres de control dentro de cadenas JSON
- fallos de parseo que en realidad solo necesitan un reintento dirigido, no un flujo completo de recuperación

Este tipo de respuesta suele terminar en código repetido de parseo, reintentos y manejo de errores dentro de la aplicación.

### Respuestas rotas típicas

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

## Qué hace

- reintentos solo cuando el error parece ser de parseo estructurado
- reparación ligera de JSON para problemas comunes y de bajo riesgo
- extracción del cuerpo JSON real desde respuestas con ruido
- limpieza de comas finales y normalización de comillas tipográficas
- escape de caracteres de control dentro de cadenas JSON
- código de llamada más pequeño y tipado
- un Spring Boot Starter listo para integrarse en proyectos con Spring AI

## Ejemplo rápido

**Sin el guard**

```java
BeanOutputConverter<MovieReview> converter = new BeanOutputConverter<>(MovieReview.class);

try {
    String raw = chatClient.prompt()
        .system(systemPrompt + "\n" + converter.getFormat())
        .user(userPrompt)
        .call()
        .content();
    return converter.convert(raw);
} catch (Exception e) {
    // retry?
    // repair json?
    // log?
    // wrap exception?
    throw e;
}
```

**Con el guard**

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

## Instalación

Este Starter ya está publicado en Maven Central.

Versión publicada actualmente:

```text
0.1.0-beta.1
```

La versión estable `0.1.0` está en preparación.

Coordenadas:

### Gradle

```groovy
implementation "io.github.kiyra-gjx:spring-ai-structured-output-guard-starter:0.1.0-beta.1"
```

### Maven

```xml
<dependency>
  <groupId>io.github.kiyra-gjx</groupId>
  <artifactId>spring-ai-structured-output-guard-starter</artifactId>
  <version>0.1.0-beta.1</version>
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

En Windows PowerShell conviene usar `curl.exe` o `Invoke-RestMethod` en lugar del alias `curl`.

## Desarrollo local

```bash
./gradlew test
./gradlew :example:bootRun
```

## Compatibilidad

El proyecto se compila y se prueba actualmente con:

- Spring Boot `4.0.1`
- Spring AI `2.0.0-M1`
- Java `21`

La línea actual de publicación sigue usando Spring AI `2.0.0-M1`. Cuando la API quede más asentada, será razonable moverla a una línea estable más reciente.

## Estado de la release

El trabajo principal que queda antes de `0.1.0` es:

1. cerrar los nombres públicos de la API y la estructura de paquetes
2. cambiar los ejemplos de instalación de `0.1.0-beta.1` a `0.1.0`
3. publicar `0.1.0`
4. añadir métricas y puntos de extensión en `0.2.x`

## Roadmap

- estrategias de reparación personalizadas
- métricas con Micrometer
- soporte para agregación de streaming
- más pruebas de integración

## Contribuciones

Se aceptan issues y pull requests. Consulta [CONTRIBUTING.md](./CONTRIBUTING.md).
