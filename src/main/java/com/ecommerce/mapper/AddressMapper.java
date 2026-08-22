package com.ecommerce.mapper;

import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.entity.Address;

import java.util.List;

public class AddressMapper {

    private AddressMapper() {
    }

    public static AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }

    public static List<AddressResponse> toResponseList(List<Address> addresses) {
        return addresses.stream().map(AddressMapper::toResponse).toList();
    }
}
