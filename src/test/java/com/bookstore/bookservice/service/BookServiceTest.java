package com.bookstore.bookservice.service;

import com.bookstore.bookservice.domain.Book;
import com.bookstore.bookservice.exception.BookAlreadyExistsException;
import com.bookstore.bookservice.exception.BookNotFoundException;
import com.bookstore.bookservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService service;

    private static final Book BOOK = new Book(
            "978-1491950357", "Cloud Native Java", "Josh Long",
            "O'Reilly", null, new BigDecimal("49.99"));

    @Test
    void viewBookListDelegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(BOOK));
        assertThat(service.viewBookList()).containsExactly(BOOK);
    }

    @Test
    void viewBookDetailsReturnsBookWhenFound() {
        when(repository.findByIsbn(BOOK.isbn())).thenReturn(Optional.of(BOOK));
        assertThat(service.viewBookDetails(BOOK.isbn())).isEqualTo(BOOK);
    }

    @Test
    void viewBookDetailsThrowsWhenNotFound() {
        when(repository.findByIsbn("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.viewBookDetails("unknown"))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void addBookToCatalogSavesAndReturnsBook() {
        when(repository.saveIfAbsent(BOOK)).thenReturn(true);
        assertThat(service.addBookToCatalog(BOOK)).isEqualTo(BOOK);
        verify(repository).saveIfAbsent(BOOK);
    }

    @Test
    void addBookToCatalogThrowsWhenIsbnExists() {
        when(repository.saveIfAbsent(BOOK)).thenReturn(false);
        assertThatThrownBy(() -> service.addBookToCatalog(BOOK))
                .isInstanceOf(BookAlreadyExistsException.class);
    }

    @Test
    void upsertBookReturnsFalseCreatedWhenIsbnExists() {
        when(repository.upsert(any())).thenReturn(false);
        BookService.UpsertResult result = service.upsertBook(BOOK.isbn(), BOOK);
        assertThat(result.created()).isFalse();
        assertThat(result.book()).isEqualTo(BOOK);
    }

    @Test
    void upsertBookReturnsTrueCreatedWhenIsbnIsNew() {
        when(repository.upsert(any())).thenReturn(true);
        BookService.UpsertResult result = service.upsertBook(BOOK.isbn(), BOOK);
        assertThat(result.created()).isTrue();
    }

    @Test
    void removeBookFromCatalogDelegatesToRepository() {
        service.removeBookFromCatalog(BOOK.isbn());
        verify(repository).deleteByIsbn(BOOK.isbn());
    }

    @Test
    void countBooksDelegatesToRepository() {
        when(repository.count()).thenReturn(3L);
        assertThat(service.countBooks()).isEqualTo(3L);
        verify(repository).count();
    }

}
