package com.libraryms.book.mapper;

import com.libraryms.author.entity.Author;
import com.libraryms.book.entity.Book;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.libraryms.author.mapper.AuthorMapper;
import com.libraryms.book.dto.BookResponse;
import com.libraryms.common.dto.summary.BookSummary;

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
