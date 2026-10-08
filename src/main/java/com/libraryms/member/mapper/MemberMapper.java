package com.libraryms.member.mapper;

import com.libraryms.member.entity.Member;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.libraryms.common.dto.summary.MemberSummary;
import com.libraryms.member.dto.MemberResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MemberMapper {

    MemberResponse toResponse(Member member);

    List<MemberResponse> toResponses(List<Member> members);

    MemberSummary toSummary(Member member);
}
