package com.libraryms.loan.mapper;

import com.libraryms.loan.entity.Loan;
import com.libraryms.book.entity.Book;
import com.libraryms.member.entity.Member;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.libraryms.book.mapper.BookMapper;
import com.libraryms.copy.mapper.BookCopyMapper;
import com.libraryms.loan.dto.LoanResponse;
import com.libraryms.member.mapper.MemberMapper;

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
