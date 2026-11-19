package edu.cs336b.library;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final ConcurrentMap<Integer, Book> books = new ConcurrentSkipListMap<>();

    @PostMapping
    public ResponseEntity<Book> addBook(@RequestBody Book book) {
        if (book.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A book ID is required.");
        }
        validateDetails(book);
        if (books.putIfAbsent(book.getId(), book) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A book with that ID already exists.");
        }
        return ResponseEntity.created(URI.create("/api/books/" + book.getId())).body(book);
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return List.copyOf(books.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Integer id) {
        return ResponseEntity.ofNullable(books.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Integer id, @RequestBody Book book) {
        if (book.getId() != null && !id.equals(book.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The JSON ID must match the path ID.");
        }
        validateDetails(book);
        book.setId(id);
        if (books.replace(id, book) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Integer id) {
        if (books.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private static void validateDetails(Book book) {
        if (isBlank(book.getTitle()) || isBlank(book.getAuthor()) || isBlank(book.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title, author, and category are required.");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Available copies must be zero or more.");
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
