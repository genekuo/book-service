package com.bookstore.bookservice.web;

import com.bookstore.bookservice.domain.Book;
import com.bookstore.bookservice.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getBooks() {
        return bookService.viewBookList();
    }

    @GetMapping("/count")
    public BookCount countBooks() {
        return new BookCount(bookService.countBooks());
    }

    public record BookCount(long count) {}

    @GetMapping("/exists/{isbn}")
    public BookExists bookExists(@PathVariable String isbn) {
        return new BookExists(bookService.bookExists(isbn));
    }

    public record BookExists(boolean exists) {}

    @GetMapping("/{isbn}")
    public ResponseEntity<Book> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(bookService.viewBookDetails(isbn));
    }

    @PostMapping
    public ResponseEntity<Book> addBook(@Valid @RequestBody Book book) {
        Book created = bookService.addBookToCatalog(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<Book> updateBook(
            @PathVariable String isbn,
            @Valid @RequestBody Book book) {
        BookService.UpsertResult result = bookService.upsertBook(isbn, book);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.book());
    }

    @DeleteMapping("/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.removeBookFromCatalog(isbn);
    }
}
