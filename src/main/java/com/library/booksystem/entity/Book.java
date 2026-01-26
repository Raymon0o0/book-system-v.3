package com.library.booksystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String genre;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "copies")
    private Integer copies = 1;

    // Новые поля для описания книги и автора
    @Column(columnDefinition = "TEXT", length = 2000)
    private String description;

    @Column(name = "author_bio", columnDefinition = "TEXT", length = 1000)
    private String authorBio;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "isbn")
    private String isbn;

    @Column(name = "pages")
    private Integer pages;

    // Конструкторы
    public Book() {}

    public Book(String title, String author, String genre, Integer publicationYear, Integer copies) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publicationYear = publicationYear;
        this.copies = copies;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Integer getCopies() { return copies; }
    public void setCopies(Integer copies) { this.copies = copies; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorBio() { return authorBio; }
    public void setAuthorBio(String authorBio) { this.authorBio = authorBio; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }
}