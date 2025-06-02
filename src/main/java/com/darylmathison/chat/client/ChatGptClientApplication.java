package com.darylmathison.chat.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ChatGptClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChatGptClientApplication.class, args);
  }
}