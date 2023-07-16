package com.gfg.ewallet.consumer;

import com.gfg.ewallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserServiceConsumer {

    @Autowired
    WalletService walletService;

    Logger logger= LoggerFactory.getLogger(UserServiceConsumer.class);

    @KafkaListener(topics = "USER_CREATED",groupId = "wallet52Grp")
    public void createNewUserWallet(String message){
        logger.info("received Message: {}",message);
        walletService.createNewWallet(message);
    }


     @KafkaListener(topics = "USER_DELETED",groupId = "wallet52Grp")
    public void disableWalletForUser(String message){
        logger.info("received Message: {}",message);
        walletService.disableActiveWallet(message);
    }

}
