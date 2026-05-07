package com.bookstore.bookservice.service;

import com.bookstore.bookservice.domain.Book;
import com.bookstore.bookservice.exception.BookAlreadyExistsException;
import com.bookstore.bookservice.exception.BookNotFoundException;
import com.bookstore.bookservice.repository.BookRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> viewBookList() {
        return repository.findAll();
    }

    public Book viewBookDetails(String isbn) {
        return repository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException(isbn));
    }

    public Book addBookToCatalog(Book book) {
        if (!repository.saveIfAbsent(book)) {
            throw new BookAlreadyExistsException(book.isbn());
        }
        return book;
    }

    public UpsertResult upsertBook(String isbn, Book book) {
        Book toSave = new Book(isbn, book.title(), book.author(),
                book.publisher(), book.publishedDate(), book.price());
        boolean isNew = repository.upsert(toSave);
        return new UpsertResult(toSave, isNew);
    }

    public void removeBookFromCatalog(String isbn) {
        repository.deleteByIsbn(isbn);
    }

    public record UpsertResult(Book book, boolean created) {}
}
