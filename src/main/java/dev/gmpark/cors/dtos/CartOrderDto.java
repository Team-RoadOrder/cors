package dev.gmpark.cors.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartOrderDto {
    private List<Long> cartIds;
}
