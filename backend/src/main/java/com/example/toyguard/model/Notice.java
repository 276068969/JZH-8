package com.example.toyguard.model;

import java.time.LocalDate;

public record Notice(Long id, String title, String content, LocalDate publishedAt) {
}
