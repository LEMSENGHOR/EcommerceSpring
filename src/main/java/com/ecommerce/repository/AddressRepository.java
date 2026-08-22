package com.ecommerce.repository;

import com.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    /** Used to enforce that a user can only read/modify their own addresses. */
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
