package com.gfg.ewallet.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfg.ewallet.service.WalletService;
import com.gfg.ewallet.service.resource.Transaction;
import jdk.security.jarsigner.JarSignerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration

public class TransactionServiceConsumer {

    ObjectMapper mapper=new ObjectMapper();

    @Autowired
    WalletService walletService;

    Logger logger= LoggerFactory.getLogger(TransactionServiceConsumer.class);


    public void handleTransactions(String message){
        logger.info("transaction message received {}",message);
        try {
            Transaction transaction = mapper.readValue(message, Transaction.class);
            walletService.updateWallet(transaction);
        }catch (JsonProcessingException ex){
            logger.error("error in deserialization the json message");
        }


    }
}
