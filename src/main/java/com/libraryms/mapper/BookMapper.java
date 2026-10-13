package com.libraryms.mapper;

import com.libraryms.model.Author;
import com.libraryms.model.Book;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.libraryms.dto.BookResponse;
import com.libraryms.dto.BookSummary;

@Mapper(componentModel = "spring", uses = {AuthorMapper.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookMapper {

    @Mapping(target = "totalCopies", constant = "0L")
    @Mapping(target = "availableCopies", constant = "0L")
    BookResponse toResponse(Book book);

    @Mapping(target = "totalCopies", source = "totalCopies")
    @Mapping(target = "availableCopies", source = "availableCopies")
    BookResponse toResponseWithCounts(Book book, long totalCopies, long availableCopies);

    BookSummary toSummary(Book book);

    List<BookSummary> toSummaries(List<Book> books);
}
