package uk.ac.warwick.courses.config
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorConfigurer
import org.springframework.context.annotation.Configuration

@EnableWebMvc
@Configuration
class Mvc extends WebMvcConfigurerAdapter {

    /*
     * WebMvcConfigurerAdapter provides various methods you can
     * override to set up message resolvers and converters and
     * that sort of thing.  
     */
  
	// Make spring set up stuff so that static content requests work
	override def configureDefaultServletHandling(cfg:DefaultServletHandlerConfigurer) = cfg.enable
	  
	override def configureInterceptors(cfg:InterceptorConfigurer) = {}
	  	
	
}