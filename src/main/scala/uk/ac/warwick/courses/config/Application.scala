package uk.ac.warwick.courses.config

import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.annotation._
import org.springframework.core.io.ClassPathResource
import org.springframework.web.servlet.view.tiles2._
import uk.ac.warwick.courses._
import org.springframework.beans.factory.annotation.Autowired

@Configuration
@Import(Array(
    classOf[Views],
    classOf[Persistence]
))
class Application {
  
    @Bean def timeController = new TimeController
	
	@Bean def properties = new org.springframework.beans.factory.config.PropertiesFactoryBean {
		setLocation(new ClassPathResource("config.properties"))
	}
	
}