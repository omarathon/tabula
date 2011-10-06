package uk.ac.warwick.courses.system

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import java.util.Properties
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.beans.FatalBeanException

/**
 * We load config.properties in our Spring config, but that is too late for Spring
 * to find the value of spring.profiles.active as it's already started making beans.
 * So we load it also in this initializer.
 */
class ContextProfileInitializer extends ApplicationContextInitializer[AnnotationConfigWebApplicationContext] {

  val configName = "courses.properties"
  val profilesProperty = "spring.profiles.active"
  
  override def initialize(ctx:AnnotationConfigWebApplicationContext) = {
    if (config.containsProperty(profilesProperty)) {
      val profiles = config.getProperty(profilesProperty).toString.split(",")
      ctx.getEnvironment().setActiveProfiles(profiles:_*)
    }
  }
    
  lazy val config = {
    val props = new Properties
    props load new ClassPathResource(configName).getInputStream
    new PropertiesPropertySource(configName, props)
  }

}