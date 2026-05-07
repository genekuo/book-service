package com.bookstore.bookservice.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Book(
        @NotBlank String isbn,
        @NotBlank String title,
        @NotBlank String author,
        String publisher,
        LocalDate publishedDate,
        @NotNull @DecimalMin("0.0") BigDecimal price
) {}
