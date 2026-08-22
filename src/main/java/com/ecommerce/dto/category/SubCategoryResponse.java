package com.ecommerce.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubCategoryResponse {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
