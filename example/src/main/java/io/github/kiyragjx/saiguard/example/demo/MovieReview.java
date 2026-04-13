package io.github.kiyragjx.saiguard.example.demo;

import java.util.List;

public record MovieReview(
    String movie,
    int score,
    List<String> strengths,
    List<String> weaknesses,
    String summary
) {
}

