package com.chatgpt.client.config;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

//@Configuration
@EnableR2dbcRepositories(basePackages = "com.chatgpt.client.repository")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

  @Bean
  @Override
  public ConnectionFactory connectionFactory() {
    return new H2ConnectionFactory(
        H2ConnectionConfiguration.builder()
            .url("mem:chatgpt_client")
            .username("sa")
            .build()
    );
  }
}
