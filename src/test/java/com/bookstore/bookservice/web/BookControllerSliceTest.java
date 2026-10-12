package com.bookstore.bookservice.web;

import com.bookstore.bookservice.domain.Book;
import com.bookstore.bookservice.exception.BookAlreadyExistsException;
import com.bookstore.bookservice.exception.BookNotFoundException;
import com.bookstore.bookservice.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    private static final Book BOOK = new Book(
            "978-1491950357", "Cloud Native Java", "Josh Long",
            "O'Reilly", null, new BigDecimal("49.99"));

    @Test
    void getBooksReturnsEmptyList() throws Exception {
        when(bookService.viewBookList()).thenReturn(List.of());
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getBooksReturnsAllBooks() throws Exception {
        when(bookService.viewBookList()).thenReturn(List.of(BOOK));
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isbn").value(BOOK.isbn()));
    }

    @Test
    void getBookByIsbnReturns200WhenFound() throws Exception {
        when(bookService.viewBookDetails(BOOK.isbn())).thenReturn(BOOK);
        mockMvc.perform(get("/books/{isbn}", BOOK.isbn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(BOOK.title()));
    }

    @Test
    void getBookByIsbnReturns404WhenNotFound() throws Exception {
        when(bookService.viewBookDetails("unknown"))
                .thenThrow(new BookNotFoundException("unknown"));
        mockMvc.perform(get("/books/{isbn}", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookExistsReturnsTrueWhenPresent() throws Exception {
        when(bookService.bookExists(BOOK.isbn())).thenReturn(true);
        mockMvc.perform(get("/books/exists/{isbn}", BOOK.isbn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
        verify(bookService).bookExists(BOOK.isbn());
    }

    @Test
    void bookExistsReturnsFalseWhenAbsent() throws Exception {
        when(bookService.bookExists("unknown")).thenReturn(false);
        mockMvc.perform(get("/books/exists/{isbn}", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
        verify(bookService).bookExists("unknown");
    }

    @Test
    void addBookReturns201OnSuccess() throws Exception {
        when(bookService.addBookToCatalog(any())).thenReturn(BOOK);
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(BOOK.isbn()));
    }

    @Test
    void addBookReturns422WhenIsbnExists() throws Exception {
        when(bookService.addBookToCatalog(any()))
                .thenThrow(new BookAlreadyExistsException(BOOK.isbn()));
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void addBookReturns400WhenTitleIsBlank() throws Exception {
        Book invalid = new Book("978-000", "", "Author", null, null, new BigDecimal("9.99"));
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addBookReturns400WhenPriceIsNegative() throws Exception {
        Book invalid = new Book("978-000", "Title", "Author", null, null, new BigDecimal("-1.00"));
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addBookReturns400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBookReturns200WhenUpdated() throws Exception {
        when(bookService.upsertBook(eq(BOOK.isbn()), any()))
                .thenReturn(new BookService.UpsertResult(BOOK, false));
        mockMvc.perform(put("/books/{isbn}", BOOK.isbn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(BOOK.isbn()));
    }

    @Test
    void updateBookReturns201WhenCreated() throws Exception {
        when(bookService.upsertBook(eq(BOOK.isbn()), any()))
                .thenReturn(new BookService.UpsertResult(BOOK, true));
        mockMvc.perform(put("/books/{isbn}", BOOK.isbn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(BOOK)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateBookReturns400WhenBodyInvalid() throws Exception {
        Book invalid = new Book(BOOK.isbn(), "", "Author", null, null, new BigDecimal("9.99"));
        mockMvc.perform(put("/books/{isbn}", BOOK.isbn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteBookReturns204() throws Exception {
        mockMvc.perform(delete("/books/{isbn}", BOOK.isbn()))
                .andExpect(status().isNoContent());
    }
}
