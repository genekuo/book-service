package com.bookstore.bookservice.repository;

import com.bookstore.bookservice.domain.Book;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    List<Book> findAll();
    Optional<Book> findByIsbn(String isbn);
    Book save(Book book);
    boolean saveIfAbsent(Book book);
    boolean upsert(Book book);
    void deleteByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    long count();
}
