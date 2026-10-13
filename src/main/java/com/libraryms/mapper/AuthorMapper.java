package com.libraryms.mapper;

import com.libraryms.model.Author;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.libraryms.dto.AuthorResponse;
import com.libraryms.dto.CreateAuthorRequest;
import com.libraryms.dto.UpdateAuthorRequest;
import com.libraryms.dto.AuthorSummary;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Author toEntity(CreateAuthorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateAuthorRequest request, @MappingTarget Author author);

    AuthorResponse toResponse(Author author);

    AuthorSummary toSummary(Author author);

    List<AuthorResponse> toResponses(List<Author> authors);
}
