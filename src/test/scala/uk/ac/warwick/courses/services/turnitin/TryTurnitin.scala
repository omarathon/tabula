package uk.ac.warwick.courses.services.turnitin

import org.apache.commons.codec.digest.DigestUtils
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import uk.ac.warwick.courses.helpers.Logging

/**
 * There should be a functional test for the Turnitin
 * support but in the meantime I'm just running this
 * little app manually to find out how it works. 
 */
object TryTurnitin extends App with Logging {
	
	val props = new Properties
	props.load(new FileInputStream( new File("local.properties") ))
	Seq("turnitin.key").foreach { key =>
		if (!props.containsKey(key)) throw new RuntimeException("Config property " + key + " not set in local.properties")
	}
	
	logger.debug("Creating instance")
	
	val api = new Turnitin
	api.sharedSecretKey = props.getProperty("turnitin.key")
	api.aid = props.getProperty("turnitin.aid")
	api.said = props.getProperty("turnitin.said")
	api.diagnostic = true
	
	logger.debug("Created instance. Creating class...")
	
	api.createClass("Automated submissions")
	api.createAssignment("Automated submissions", "Test assignment")
	
	logger.debug("Created class, maybe")
	
}