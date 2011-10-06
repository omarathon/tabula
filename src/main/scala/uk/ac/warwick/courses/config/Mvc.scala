package uk.ac.warwick.courses.config
import org.springframework.context.annotation.Configuration
import org.springframework.context.EnvironmentAware
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import uk.ac.warwick.courses.helpers.EnvironmentAwareness
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor

@EnableWebMvc
@Configuration
class Mvc extends WebMvcConfigurerAdapter with EnvironmentAwareness {

    /*
     * WebMvcConfigurerAdapter provides various methods you can
     * override to set up message resolvers and converters and
     * that sort of thing.  
     */
  
	// Make spring set up stuff so that static content requests work
	override def configureDefaultServletHandling(cfg:DefaultServletHandlerConfigurer) =
	  if (environment.acceptsProfiles("production","dev")) {
	    cfg.enable()
	  }
	
//	  
//	override def configureInterceptors(cfg:InterceptorConfigurer) = 
//	  cfg.addInterceptor(new OpenSessionInViewInterceptor)
//	
	  	
	
}