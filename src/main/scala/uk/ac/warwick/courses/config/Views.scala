package uk.ac.warwick.courses.config
import java.lang.Object
import java.util.Properties

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import org.springframework.web.servlet.view.tiles2.TilesView

import uk.ac.warwick.courses.helpers.ImplicitConversions
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.SmartDefinitionsFactory

@Configuration
@Profile(Array("production", "dev"))
class Views extends Object with Logging with ImplicitConversions {
  
  @Bean def tilesConfigurer = new org.springframework.web.servlet.view.tiles2.TilesConfigurer {
    setDefinitions(Array("/WEB-INF/defs/views.xml"))
    setDefinitionsFactoryClass(classOf[SmartDefinitionsFactory])
  }

  @Bean def freemarkerConfigurer = new FreeMarkerConfigurer {
    setTemplateLoaderPath("/WEB-INF/freemarker/")
    setFreemarkerSettings(new Properties {
      setProperty("default_encoding", "UTF-8")
      setProperty("output_encoding", "UTF-8")
    })
  }

  @Bean def viewResolver = new org.springframework.web.servlet.view.UrlBasedViewResolver {
    setViewClass(classOf[org.springframework.web.servlet.view.tiles2.TilesView])
  }
}