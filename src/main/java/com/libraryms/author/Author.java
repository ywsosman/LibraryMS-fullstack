package com.libraryms.author;

import java.time.LocalDate;

import com.libraryms.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Author extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public Author(String name) {
        this.name = name;
    }

    public Author(String name, String bio, LocalDate birthDate) {
        this.name = name;
        this.bio = bio;
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "Author{id=" + getId() + ", name='" + name + "'}";
    }
}
