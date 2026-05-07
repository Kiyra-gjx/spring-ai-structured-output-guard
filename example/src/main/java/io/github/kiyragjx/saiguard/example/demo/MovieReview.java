package io.github.kiyragjx.saiguard.example.demo;

import java.util.List;

/**
 * Structured movie review used by the example endpoints.
 *
 * @param movie movie title
 * @param score review score from 0 to 100
 * @param strengths positive review points
 * @param weaknesses negative review points
 * @param summary short review summary
 */
public record MovieReview(
    String movie,
    int score,
    List<String> strengths,
    List<String> weaknesses,
    String summary
) {
}
