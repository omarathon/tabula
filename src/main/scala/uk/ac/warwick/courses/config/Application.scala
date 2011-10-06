package uk.ac.warwick.courses.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.context.annotation.ComponentScan

@Configuration
@EnableScheduling
@EnableTransactionManagement
class Application {
  
}


object Application {

  @Bean def properties = new PropertySourcesPlaceholderConfigurer {
    setLocation(new ClassPathResource("courses.properties"))
  }
  
}