package com.libraryms.mapper;

import com.libraryms.model.Book;
import com.libraryms.model.BookCopy;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.libraryms.dto.CopySummary;
import com.libraryms.dto.CopyResponse;

@Mapper(componentModel = "spring", uses = {BookMapper.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookCopyMapper {

    CopyResponse toResponse(BookCopy copy);

    CopySummary toSummary(BookCopy copy);

    List<CopyResponse> toResponses(List<BookCopy> copies);
}
