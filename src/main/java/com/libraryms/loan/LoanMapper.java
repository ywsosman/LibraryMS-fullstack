package com.libraryms.loan;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.libraryms.book.BookMapper;
import com.libraryms.copy.BookCopyMapper;
import com.libraryms.loan.dto.LoanResponse;
import com.libraryms.member.MemberMapper;

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
