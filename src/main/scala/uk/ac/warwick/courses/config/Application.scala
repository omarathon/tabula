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
@ComponentScan(Array(
	"uk.ac.warwick.courses.controllers",
	"uk.ac.warwick.courses.data"
))
class Application {
  
}

/* stuff in an object is the same as static stuff in a class. 
 */
object Application {
  
  @Bean def properties = new PropertySourcesPlaceholderConfigurer {
    setLocation(new ClassPathResource("config.properties"))
  }
  
}