package uk.ac.warwick.courses.system

import java.util.Properties

import scala.collection.JavaConversions._

import org.springframework.context.ApplicationContextInitializer
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.web.context.ConfigurableWebApplicationContext

import uk.ac.warwick.courses.helpers.Logging


/**
 * Sets some properties on the application context before we start
 * loading any beans and stuff.
 */
class ContextSetupInitializer extends ApplicationContextInitializer[ConfigurableWebApplicationContext] with Logging {

  override def initialize(ctx:ConfigurableWebApplicationContext) = {
	  
  }
  
}