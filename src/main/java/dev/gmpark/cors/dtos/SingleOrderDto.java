package dev.gmpark.cors.dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SingleOrderDto {
    private Long itemId;
    private String size;
    private List<Long> cartIds;
    private String request;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String addressDetail;
}
