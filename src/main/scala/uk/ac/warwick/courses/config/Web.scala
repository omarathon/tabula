package uk.ac.warwick.courses.config
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import uk.ac.warwick.courses.TimeController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@Configuration
@EnableScheduling
@EnableTransactionManagement
@Profile(Array("dev","production"))
class Web {
  
  @Bean def timeController = new TimeController

}