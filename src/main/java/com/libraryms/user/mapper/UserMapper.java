package com.libraryms.user.mapper;

import com.libraryms.user.entity.User;
import com.libraryms.member.entity.Member;
import com.libraryms.user.entity.Role;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.libraryms.user.dto.UserResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleNames")
    UserResponse toResponse(User user);

    List<UserResponse> toResponses(List<User> users);

    @Named("mapRoleNames")
    default Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
    }
}
