package com.gfg.ewallet.service.resource;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Transaction {

    private Long senderId;
    private Long receiverId;
    private Double amount;
    private String status;

}
