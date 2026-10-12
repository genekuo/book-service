package com.bookstore.bookservice.web;

import com.bookstore.bookservice.domain.Book;
import com.bookstore.bookservice.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private static final Book BOOK = new Book(
            "978-1491950357", "Cloud Native Java", "Josh Long",
            "O'Reilly", null, new BigDecimal("49.99"));

    @BeforeEach
    void setUp() {
        bookRepository.findAll().forEach(b -> bookRepository.deleteByIsbn(b.isbn()));
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        // Initially empty
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Create
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(BOOK.isbn()));

        // Read
        mockMvc.perform(get("/books/{isbn}", BOOK.isbn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(BOOK.title()));

        // Duplicate create → 422
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isUnprocessableContent());

        // Update
        Book updated = new Book(BOOK.isbn(), "Cloud Native Java 2nd Ed",
                BOOK.author(), BOOK.publisher(), BOOK.publishedDate(), new BigDecimal("59.99"));
        mockMvc.perform(put("/books/{isbn}", BOOK.isbn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cloud Native Java 2nd Ed"));

        // Delete
        mockMvc.perform(delete("/books/{isbn}", BOOK.isbn()))
                .andExpect(status().isNoContent());

        // Confirm deleted
        mockMvc.perform(get("/books/{isbn}", BOOK.isbn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void putCreatesBookWhenIsbnIsNew() throws Exception {
        mockMvc.perform(put("/books/{isbn}", BOOK.isbn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(BOOK.isbn()));
    }

    @Test
    void bookExistsReflectsRepositoryState() throws Exception {
        Book book = new Book("978-0000000001", "Exists Check", "Author",
                null, null, new BigDecimal("9.99"));

        mockMvc.perform(get("/books/exists/{isbn}", book.isbn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(book)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/books/exists/{isbn}", book.isbn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void getUnknownIsbnReturns404() throws Exception {
        mockMvc.perform(get("/books/{isbn}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
