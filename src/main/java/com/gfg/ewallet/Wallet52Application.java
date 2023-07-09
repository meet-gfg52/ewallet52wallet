package com.gfg.ewallet;

import com.gfg.ewallet.service.WalletService;
import com.gfg.ewallet.service.impl.WalletServiceImpl;
import com.gfg.ewallet.service.resource.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Wallet52Application implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(Wallet52Application.class, args);
	}

	@Autowired
	WalletServiceImpl service;

	@Override
	public void run(String... args) throws Exception {
		for(int i=0;i<10;i++) {
			Transaction transaction = new Transaction();
			transaction.setAmount(1.0);
			transaction.setSenderId(52L);
			transaction.setReceiverId(2L);
			try {
				service.performTransaction(transaction);
			} catch (Exception ex) {
				System.out.println("transaction ended in exception");
			}
		}

	}
}
