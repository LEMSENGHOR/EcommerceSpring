package com.ecommerce.mapper;

import com.ecommerce.dto.role.RoleResponse;
import com.ecommerce.entity.Role;

import java.util.List;

public class RoleMapper {

    private RoleMapper() {
    }

    public static RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName().name())
                .build();
    }

    public static List<RoleResponse> toResponseList(List<Role> roles) {
        return roles.stream().map(RoleMapper::toResponse).toList();
    }
}
