package com.bookstore.bookservice.repository;

import com.bookstore.bookservice.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBookRepositoryTest {

    private InMemoryBookRepository repository;

    private static final Book BOOK = new Book(
            "978-1491950357", "Cloud Native Java", "Josh Long",
            "O'Reilly", null, new BigDecimal("49.99"));

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookRepository();
    }

    @Test
    void findAllOnEmptyStoreReturnsEmptyList() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void saveAndFindAllReturnsBook() {
        repository.save(BOOK);
        assertThat(repository.findAll()).containsExactly(BOOK);
    }

    @Test
    void findByIsbnReturnsEmptyForUnknownIsbn() {
        assertThat(repository.findByIsbn("unknown")).isEmpty();
    }

    @Test
    void findByIsbnReturnsBookAfterSave() {
        repository.save(BOOK);
        assertThat(repository.findByIsbn(BOOK.isbn())).contains(BOOK);
    }

    @Test
    void saveTwiceWithSameIsbnOverwrites() {
        repository.save(BOOK);
        Book updated = new Book(BOOK.isbn(), "Updated Title", BOOK.author(),
                BOOK.publisher(), BOOK.publishedDate(), BOOK.price());
        repository.save(updated);
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findByIsbn(BOOK.isbn())).contains(updated);
    }

    @Test
    void existsByIsbnReturnsFalseWhenAbsent() {
        assertThat(repository.existsByIsbn(BOOK.isbn())).isFalse();
    }

    @Test
    void existsByIsbnReturnsTrueAfterSave() {
        repository.save(BOOK);
        assertThat(repository.existsByIsbn(BOOK.isbn())).isTrue();
    }

    @Test
    void deleteByIsbnRemovesBook() {
        repository.save(BOOK);
        repository.deleteByIsbn(BOOK.isbn());
        assertThat(repository.findByIsbn(BOOK.isbn())).isEmpty();
    }

    @Test
    void deleteByIsbnOnNonExistentIsbnIsNoOp() {
        repository.deleteByIsbn("does-not-exist");
        assertThat(repository.findAll()).isEmpty();
    }
}
