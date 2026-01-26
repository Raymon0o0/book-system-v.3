package com.library.booksystem.repository;

import com.library.booksystem.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Поиск книг по жанру, исключая текущую книгу
    List<Book> findByGenreAndIdNot(String genre, Long id);

    // Поиск книг по автору, исключая текущую книгу
    List<Book> findByAuthorAndIdNot(String author, Long id);

    // Получение случайных книг (для случаев, когда мало похожих книг)
    @Query(value = "SELECT * FROM books WHERE id != :id ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Book> findRandomBooks(@Param("id") Long id, @Param("limit") int limit);

    // Поиск книг с похожим названием (для расширения функционала)
    List<Book> findByTitleContainingIgnoreCase(String title);
}