package uk.ac.warwick.tabula.services

import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import org.springframework.web.context.ServletContextAware
import javax.servlet.ServletContext
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.SSOConfigLoader
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.cache.DatabaseUserCache
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.QueueListener
import uk.ac.warwick.util.queue.conversion.ItemType
import scala.react.Observing

/**
 * TAB-106 This bean registers an observer to the maintenance mode service, 
 * which swaps out the SSO configuration to old mode SSO whenever maintenance 
 * mode is enabled, and swaps it back to new mode when it's disabled.
 * 
 * This listener both listens to the queue and observes, as the queue listener
 * automatically removes messages from the same source, which we don't want.
 */
class SSOMaintenanceModeListener extends QueueListener with Observing with InitializingBean with Logging with ServletContextAware {
	
	var maintenanceModeService = Wire.auto[MaintenanceModeService]
	var queue = Wire.named[Queue]("settingsSyncTopic")
	
	lazy val config = servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY) match {
		case config: SSOConfiguration => Some(config)
		case _ => None
	}
	lazy val cache = servletContext.getAttribute(SSOConfigLoader.SSO_CACHE_KEY) match {
		case cache: DatabaseUserCache => Some(cache)
		case _ => None
	}
	
	def switchToOldMode {
		config map { config => 
			if ("new".equals(config.getString("mode"))) {
				config.setProperty("mode", "old")
				config.setProperty("origin.login.location", "https://websignon.warwick.ac.uk/origin/slogin")
			}
		}
			
		// Turn database clustering off as well
		cache map { _.setDatabaseEnabled(false) }
	}
	
	def switchToNewMode {
		config map { config => 
			if ("old".equals(config.getString("mode"))) {
				config.setProperty("mode", "new")
				config.setProperty("origin.login.location", "https://websignon.warwick.ac.uk/origin/hs")
			}
		}
		
		// Turn database clustering back on as well
		cache map { _.setDatabaseEnabled(true) }
	}
	
	override def isListeningToQueue = true
	override def onReceive(item: Any) {	
		item match {
			case message: MaintenanceModeMessage =>
				if (message.enabled) switchToOldMode
				else switchToNewMode
		}
	}
		
	override def afterPropertiesSet {
		queue.addListener(classOf[MaintenanceModeMessage].getAnnotation(classOf[ItemType]).value, this)
		
		observe(maintenanceModeService.changingState) { enabled => 
			if (enabled) switchToOldMode
			else switchToNewMode
			true
		}
	}
	
	private var servletContext: ServletContext = _
	
	override def setServletContext(servletContext: ServletContext) {
		this.servletContext = servletContext
	}

}