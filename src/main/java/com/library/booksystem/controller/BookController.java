package com.library.booksystem.controller;

import com.library.booksystem.entity.Book;
import com.library.booksystem.repository.BookRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    // ИСПРАВЛЕННЫЙ КОД
    @GetMapping("/")
    public String showHomePage(Model model,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) String author,
                               @RequestParam(required = false) String genre,
                               @RequestParam(required = false) Integer year) {

        List<Book> books;

        if (title != null && !title.isEmpty() ||
                author != null && !author.isEmpty() ||
                genre != null && !genre.isEmpty() ||
                year != null) {

            books = bookRepository.findAll().stream()
                    .filter(book ->
                            (title == null || title.isEmpty() ||
                                    book.getTitle().toLowerCase().contains(title.toLowerCase())) &&
                                    (author == null || author.isEmpty() ||
                                            book.getAuthor().toLowerCase().contains(author.toLowerCase())) &&
                                    (genre == null || genre.isEmpty() ||
                                            book.getGenre().equals(genre)) &&
                                    (year == null || book.getPublicationYear().equals(year))
                    )
                    .collect(Collectors.toList());
        } else {
            books = bookRepository.findAll();
        }

        // Получаем все уникальные жанры для выпадающего списка
        List<String> allGenres = bookRepository.findAll().stream()
                .map(Book::getGenre)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Получаем все уникальные года для выпадающего списка
        List<Integer> allYears = bookRepository.findAll().stream()
                .map(Book::getPublicationYear)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Добавляем атрибуты в модель
        model.addAttribute("books", books);
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allYears", allYears);
        model.addAttribute("searchTitle", title);
        model.addAttribute("searchAuthor", author);
        model.addAttribute("searchGenre", genre);
        model.addAttribute("searchYear", year);

        return "index";
    }

    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add-book";
    }

    @PostMapping("/save")   
    public String saveBook(@Valid @ModelAttribute Book book, BindingResult result) {
        if (result.hasErrors()) {
            return "add-book";
        }
        bookRepository.save(book);
        return "redirect:/";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID книги: " + id));

        // Получаем все уникальные жанры для выпадающего списка
        List<String> genres = bookRepository.findAll().stream()
                .map(Book::getGenre)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        model.addAttribute("book", book);
        model.addAttribute("genres", genres);
        return "edit-book";
    }

    @PostMapping("/update/{id}")
    public String updateBook(@PathVariable Long id, @Valid @ModelAttribute Book book,
                             BindingResult result) {
        if (result.hasErrors()) {
            return "edit-book";
        }
        book.setId(id);
        bookRepository.save(book);
        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/book/{id}")
    public String showBookDetails(@PathVariable Long id, Model model) {
        Optional<Book> optionalBook = bookRepository.findById(id);

        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            model.addAttribute("book", book);

            // Получаем похожие книги (используем расширенный метод с весами)
            List<Book> similarBooks = findSimilarBooksAdvanced(book, 5);
            model.addAttribute("similarBooks", similarBooks);

            return "book-details";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/stats")
    public String showStatistics(Model model) {
        List<Book> allBooks = bookRepository.findAll();

        // Общее количество книг
        long totalBooks = allBooks.size();

        // Общее количество копий
        int totalCopies = allBooks.stream()
                .mapToInt(Book::getCopies)
                .sum();

        // Книги по жанрам
        Map<String, Long> booksByGenre = allBooks.stream()
                .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalCopies", totalCopies);
        model.addAttribute("booksByGenre", booksByGenre);

        return "stats";
    }

    @GetMapping("/database")
    public String showDatabaseInfo(Model model) {
        model.addAttribute("dbUrl", "jdbc:h2:mem:librarydb");  // Важно: mem, а не file
        model.addAttribute("dbUser", "librarian");
        model.addAttribute("dbPassword", "secret123");
        model.addAttribute("h2ConsoleUrl", "/h2-console");
        return "database-info";
    }

    @GetMapping("/search")
    public String searchBooks(@RequestParam(required = false) String title,
                              @RequestParam(required = false) String author,
                              @RequestParam(required = false) String genre,
                              @RequestParam(required = false) Integer year,
                              Model model) {
        return showHomePage(model, title, author, genre, year);
    }

    /**
     * Метод для поиска похожих книг (базовый)
     */
    private List<Book> findSimilarBooks(Book book, int limit) {
        Set<Book> similarBooks = new LinkedHashSet<>();

        // 1. Ищем книги того же жанра (исключая текущую)
        List<Book> sameGenre = bookRepository.findByGenreAndIdNot(book.getGenre(), book.getId());
        similarBooks.addAll(sameGenre);

        // 2. Если книг того же жанра недостаточно, добавляем книги того же автора
        if (similarBooks.size() < limit) {
            List<Book> sameAuthor = bookRepository.findByAuthorAndIdNot(book.getAuthor(), book.getId());
            for (Book authorBook : sameAuthor) {
                if (similarBooks.size() >= limit) break;
                if (!similarBooks.contains(authorBook)) {
                    similarBooks.add(authorBook);
                }
            }
        }

        // 3. Если все еще недостаточно книг, добавляем случайные книги
        if (similarBooks.size() < limit) {
            int remaining = limit - similarBooks.size();
            List<Book> randomBooks = bookRepository.findRandomBooks(book.getId(), remaining);
            for (Book randomBook : randomBooks) {
                if (!similarBooks.contains(randomBook)) {
                    similarBooks.add(randomBook);
                }
            }
        }

        // Преобразуем Set в List и ограничиваем лимитом
        List<Book> result = new ArrayList<>(similarBooks);
        return result.subList(0, Math.min(result.size(), limit));
    }

    /**
     * Расширенный метод для поиска похожих книг с учетом весов (шаг 5)
     */
    private List<Book> findSimilarBooksAdvanced(Book book, int limit) {
        // Создаем Map для хранения книг и их "веса" похожести
        Map<Book, Integer> bookScores = new HashMap<>();

        // 1. Книги того же жанра получают 3 балла
        List<Book> sameGenre = bookRepository.findByGenreAndIdNot(book.getGenre(), book.getId());
        for (Book b : sameGenre) {
            bookScores.put(b, bookScores.getOrDefault(b, 0) + 3);
        }

        // 2. Книги того же автора получают 2 балла
        List<Book> sameAuthor = bookRepository.findByAuthorAndIdNot(book.getAuthor(), book.getId());
        for (Book b : sameAuthor) {
            bookScores.put(b, bookScores.getOrDefault(b, 0) + 2);
        }

        // 3. Книги с похожим названием получают 1 балл (если нужно)
        // Ищем книги, в названии которых есть первое слово из названия текущей книги
        String firstWord = book.getTitle().split(" ")[0];
        List<Book> similarTitle = bookRepository.findAll().stream()
                .filter(b -> !b.getId().equals(book.getId()) &&
                        b.getTitle().toLowerCase().contains(firstWord.toLowerCase()))
                .collect(Collectors.toList());

        for (Book b : similarTitle) {
            bookScores.put(b, bookScores.getOrDefault(b, 0) + 1);
        }

        // Сортируем книги по убыванию баллов
        List<Book> sortedBooks = bookScores.entrySet().stream()
                .sorted(Map.Entry.<Book, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Если книг недостаточно, добавляем случайные
        if (sortedBooks.size() < limit) {
            int remaining = limit - sortedBooks.size();
            List<Book> randomBooks = bookRepository.findRandomBooks(book.getId(), remaining);
            for (Book randomBook : randomBooks) {
                if (!sortedBooks.contains(randomBook)) {
                    sortedBooks.add(randomBook);
                }
            }
        }

        // Ограничиваем результат
        return sortedBooks.subList(0, Math.min(sortedBooks.size(), limit));
    }
}