package uk.ac.warwick.courses.config
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import uk.ac.warwick.courses.controllers._
import org.springframework.context.annotation.ComponentScan

@Configuration
@EnableScheduling
@EnableTransactionManagement
@Profile(Array("dev","production"))
@ComponentScan(Array("uk.ac.warwick.courses.controllers"))
class Web {
//  @Bean def homeController = new HomeController
//  @Bean def timeController = new TimeController
}