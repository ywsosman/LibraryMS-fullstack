package com.libraryms.copy.mapper;

import com.libraryms.book.entity.Book;
import com.libraryms.copy.entity.BookCopy;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.libraryms.book.mapper.BookMapper;
import com.libraryms.common.dto.summary.CopySummary;
import com.libraryms.copy.dto.CopyResponse;

@Mapper(componentModel = "spring", uses = {BookMapper.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookCopyMapper {

    CopyResponse toResponse(BookCopy copy);

    CopySummary toSummary(BookCopy copy);

    List<CopyResponse> toResponses(List<BookCopy> copies);
}
