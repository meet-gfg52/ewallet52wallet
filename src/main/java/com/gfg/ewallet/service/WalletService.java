package com.gfg.ewallet.service;

import com.gfg.ewallet.domain.Wallet;
import com.gfg.ewallet.service.resource.Transaction;

public interface WalletService {

    Wallet getUserWallet(String userId);

    Wallet createNewWallet(String userId);

    Wallet disableActiveWallet(String userId);

    void updateWallet(Transaction transaction);
}
