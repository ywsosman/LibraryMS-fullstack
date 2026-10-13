package com.libraryms.mapper;

import com.libraryms.model.Member;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.libraryms.dto.MemberSummary;
import com.libraryms.dto.MemberResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MemberMapper {

    MemberResponse toResponse(Member member);

    List<MemberResponse> toResponses(List<Member> members);

    MemberSummary toSummary(Member member);
}
