package com.bookstore.bookservice.repository;

import com.bookstore.bookservice.domain.Book;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookRepository implements BookRepository {

    private final Map<String, Book> store = new ConcurrentHashMap<>();

    @Override
    public List<Book> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(store.get(isbn));
    }

    @Override
    public Book save(Book book) {
        store.put(book.isbn(), book);
        return book;
    }

    @Override
    public boolean saveIfAbsent(Book book) {
        return store.putIfAbsent(book.isbn(), book) == null;
    }

    @Override
    public boolean upsert(Book book) {
        return store.put(book.isbn(), book) == null;
    }

    @Override
    public void deleteByIsbn(String isbn) {
        store.remove(isbn);
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return store.containsKey(isbn);
    }
    @Override
    public long count() {
        return store.size();
    }
}
