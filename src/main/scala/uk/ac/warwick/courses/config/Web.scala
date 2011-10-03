package uk.ac.warwick.courses.config
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import uk.ac.warwick.courses.TimeController
import org.springframework.context.annotation.Bean

@Configuration
@EnableScheduling
@EnableTransactionManagement
@Import(Array(
  classOf[Views],
  classOf[Mvc]))
class Web {
  
  @Bean def timeController = new TimeController

}