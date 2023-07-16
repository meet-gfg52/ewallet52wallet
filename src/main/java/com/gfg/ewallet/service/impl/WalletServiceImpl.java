package com.gfg.ewallet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gfg.ewallet.domain.Wallet;
import com.gfg.ewallet.repository.WalletRepository;
import com.gfg.ewallet.service.WalletService;
import com.gfg.ewallet.service.resource.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Random;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    KafkaTemplate kafkaTemplate;

    private final String wallet_update_topic="WALLET_UPDATE";

    private ObjectMapper mapper=new ObjectMapper();

    Logger logger= LoggerFactory.getLogger(WalletServiceImpl.class);

    @Override
    public Wallet getUserWallet(String userId) {
        return walletRepository.findByUserId(Long.valueOf(userId));
    }

    @Override
    public Wallet createNewWallet(String userId) {
        Wallet wallet=new Wallet();
        wallet.setUserId(Long.valueOf(userId));
        wallet.setBalance(0.0);
        wallet.setActive(true);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet disableActiveWallet(String userId) {
        Wallet wallet=walletRepository.findByUserId(Long.valueOf(userId));
        if(Objects.nonNull(wallet)){
            wallet.setActive(false);
            //if balance not zero, trigger bank transaction
            return walletRepository.save(wallet);
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = NullPointerException.class , noRollbackFor = ArithmeticException.class)
    public void updateWallet(Transaction transaction) throws JsonProcessingException {
        try {


            if (Objects.nonNull(transaction)
                    && transaction.getSenderId().equals(-99L)) {

                Wallet receiverWallet = walletRepository.findByUserId(transaction.getReceiverId());
                if (Objects.isNull(receiverWallet)) {
                    logger.error("Invalid receiver Id");
                }
                updateUserWallet(receiverWallet, transaction.getAmount());

            } else if (Objects.nonNull(transaction)
                    && transaction.getReceiverId().equals(-99L)) {
                Wallet senderWallet = walletRepository.findByUserId(transaction.getSenderId());
                if (Objects.isNull(senderWallet)) {
                    logger.error("Invalid Sender Id");
                }
                updateUserWallet(senderWallet, -1 * transaction.getAmount());
            } else if (Objects.nonNull(transaction)) {
                Wallet receiverWallet = walletRepository.findByUserId(transaction.getReceiverId());
                if (Objects.isNull(receiverWallet)) {
                    logger.error("Invalid receiver Id");
                }
                Wallet senderWallet = walletRepository.findByUserId(transaction.getSenderId());
                if (Objects.isNull(senderWallet)) {
                    logger.error("Invalid Sender Id");
                }
                performTransaction(senderWallet, receiverWallet, transaction.getAmount());
            } else {
                logger.error("Invalid Transaction Status");
            }
            transaction.setStatus("SUCCESS");
        }catch (Exception ex){
            transaction.setStatus("FAILURE");
        }finally {
            kafkaTemplate.send(wallet_update_topic,mapper.writeValueAsString(transaction));
        }
    }

    //@Transactional(propagation = Propagation.REQUIRED,rollbackFor = NullPointerException.class , noRollbackFor = ArithmeticException.class)
    private void performTransaction(Wallet senderWallet,Wallet receiverWallet,Double amount) {
       try {

           Wallet senderWalletCopy = new Wallet();
           Wallet receiverWalletCopy = new Wallet();
           BeanUtils.copyProperties(receiverWallet, receiverWalletCopy);
           BeanUtils.copyProperties(senderWallet, senderWalletCopy);

           logger.info("starting transaction between sender {} and receiver {}", senderWallet.getUserId(), receiverWallet.getUserId());

           senderWalletCopy.setBalance(senderWallet.getBalance() - amount);

           receiverWalletCopy.setBalance(receiverWalletCopy.getBalance() + amount);

           walletRepository.save(senderWalletCopy);
           walletRepository.save(receiverWalletCopy);
       }catch (Exception ex){
           logger.error("exception while updating balance");
          // walletRepository.save(receiverWallet);
          // walletRepository.save(senderWallet);
           throw ex;
       }

    }

    private void updateUserWallet(Wallet receiverWallet, Double amount) {
        try {
            Wallet receiverWalletCopy = new Wallet();
            BeanUtils.copyProperties(receiverWallet, receiverWalletCopy);
            receiverWalletCopy.setBalance(receiverWalletCopy.getBalance() + amount);
            walletRepository.save(receiverWalletCopy);
        }catch (Exception ex){
            logger.error("exception while updating balance");
            walletRepository.save(receiverWallet);
        }
    }


    /**
     *
     * Identify the type load/withdraw/transfer
     * if sender and receiver are valid.
     *
     *
     * 1. Load money
     *  sender -99
     *  receiver user's wallet
     *  update user wallet with amount in transaction.
     *
     *  2. withdraw money
     *  sender user's wallet
     *  receiver is -99
     *  update user wallet with amount in transaction.
     *
     *  3.Transfer
     *  check if sender has sufficient balance in wallet
     *  deduct the amount from sender's wallet
     *  add the amount in the receiver's wallet
     *
     *  incase of any failure rollback the state of both wallets
     *
     * */

    /***
     * Types of propogation:
     * REQUIRED: This is the default propagation type. It means that if a transaction already exists, the called method will join that transaction. Otherwise, a new transaction will be created for the called method.
     *
     * REQUIRES_NEW: This propagation type always creates a new transaction for the called method. It suspends the current transaction, if one exists, and starts a new independent transaction. Once the called method completes, the suspended transaction, if any, is resumed.
     *
     * MANDATORY: This propagation type indicates that a transaction must already exist when the called method is invoked. If no transaction exists, an exception will be thrown.
     *
     * NESTED: This propagation type creates a nested transaction within an existing transaction. It's similar to REQUIRED, but the nested transaction can be rolled back independently of the outer transaction. If the outer transaction is rolled back, the nested transaction is also rolled back. However, if the nested transaction is rolled back, it doesn't affect the outer transaction.
     *
     * SUPPORTS: This propagation type supports the calling method's transaction. If a transaction exists, the called method will be executed within that transaction. If no transaction exists, the called method will be executed non-transactionally.
     *
     * NOT_SUPPORTED: This propagation type specifies that the called method should not be executed within a transaction. If a transaction exists, it will be suspended for the duration of the called method's execution.
     *
     * NEVER: This propagation type ensures that the called method is not executed within a transaction. If a transaction is active, an exception will be thrown.
     *
     * NESTED_READ_COMMITTED: This propagation type is similar to NESTED, but it enforces a read-committed isolation level for the nested transaction.
     * */
}
