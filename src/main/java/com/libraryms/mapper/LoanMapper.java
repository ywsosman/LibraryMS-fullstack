package com.libraryms.mapper;

import com.libraryms.model.Loan;
import com.libraryms.model.Book;
import com.libraryms.model.Member;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.libraryms.dto.LoanResponse;

@Mapper(componentModel = "spring",
        uses = {BookCopyMapper.class, BookMapper.class, MemberMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LoanMapper {

    @Mapping(target = "copy", source = "copy")
    @Mapping(target = "book", source = "copy.book")
    @Mapping(target = "member", source = "member")
    @Mapping(target = "open", expression = "java(loan.isOpen())")
    @Mapping(target = "overdue", expression = "java(loan.isOpen() && java.time.Instant.now().isAfter(loan.getDueDate()))")
    LoanResponse toResponse(Loan loan);

    List<LoanResponse> toResponses(List<Loan> loans);
}
