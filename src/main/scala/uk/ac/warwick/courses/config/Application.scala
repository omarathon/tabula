package uk.ac.warwick.courses.config

import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableScheduling
@EnableTransactionManagement
@Import(Array(
  classOf[Persistence]
  ))
class Application {

  @Bean def properties = new PropertiesFactoryBean {
    setLocation(new ClassPathResource("config.properties"))
  }
  
}