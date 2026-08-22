package com.ecommerce.service.impl;

import com.ecommerce.dto.role.RoleResponse;
import com.ecommerce.mapper.RoleMapper;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponse> getAllRoles() {
        return RoleMapper.toResponseList(roleRepository.findAll());
    }
}
