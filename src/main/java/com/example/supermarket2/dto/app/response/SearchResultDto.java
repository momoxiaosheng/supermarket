package com.example.supermarket2.dto.app.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SearchResultDto {
    private Long id;
    private String name;
    private String description;
    private String image;
    private BigDecimal price;
    private String location;
}
