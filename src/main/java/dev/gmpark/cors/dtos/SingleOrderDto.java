package dev.gmpark.cors.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleOrderDto {
    private Long itemId;
    private String size;
    private String request;
}
