package uk.ac.warwick.courses.services.turnitin

import org.apache.commons.codec.digest.DigestUtils
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.TestHelpers
import java.io.FileWriter
import uk.ac.warwick.courses.services.turnitin._

/**
 * There should be a functional test for the Turnitin
 * support but in the meantime I'm just running this
 * little app manually to find out how it works. 
 */
object TryTurnitin extends App with Logging with TestHelpers {
	
	val api = new Turnitin
	try {
		emptyTempDirSet
	
		val props = new Properties
		props.load(new FileInputStream( new File("local.properties") ))
		Seq("turnitin.key").foreach { key =>
			if (!props.containsKey(key)) throw new RuntimeException("Config property " + key + " not set in local.properties")
		}
		
		logger.debug("Creating instance")
		
		api.sharedSecretKey = props.getProperty("turnitin.key")
		api.aid = props.getProperty("turnitin.aid")
		api.said = props.getProperty("turnitin.said")
		api.diagnostic = false
		
		val f = new File("src/test/resources/turnitin-submission.doc")
		if (!f.exists) throw new IllegalArgumentException("Whoops, test file doesn't exist")
		
		val deleted = api.deleteAssignment("Automated submissions", "Test assignment")
		
		println(deleted)
		
		//api.createClass("Automated submissions")
		/*api.createAssignment("Automated submissions", "Test assignment", update=true)
		val result = api.submitPaper("Automated submissions", "Test assignment", "Test paper", f, "Coursework", "App")
		println(result)
		
		result match {
			case Created(id) => {
				println("Going to try using id " + id)
				val report = api.getReport(id)
				println(report)
			}
			case _ => println("Oh, we didn't submit a paper or something.")
		}*/
		
	
	} finally {
		deleteTemporaryDirs
		api.destroy()
	}
	
}