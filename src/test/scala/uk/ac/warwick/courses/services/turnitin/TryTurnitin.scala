package uk.ac.warwick.courses.services.turnitin

import org.apache.commons.codec.digest.DigestUtils
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.TestHelpers
import java.io.FileWriter
import uk.ac.warwick.courses.services.turnitin._
import uk.ac.warwick.courses.commands.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.courses.TestFixtures
import uk.ac.warwick.courses.data.model.Submission

/**
 * There should be a functional test for the Turnitin
 * support but in the meantime I'm just running this
 * little app manually to find out how it works. 
 */
object TryTurnitin extends App with Logging with TestHelpers with TestFixtures {
	
	val api = new Turnitin
	try {
		emptyTempDirSet
		
		setupAspects
	
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
		
		/*val deleted = api.deleteAssignment("Automated submissions", "Test assignment")
		
		println(deleted)
		
		//api.createClass("Automated submissions")
		api.createAssignment("Automated submissions", "Test assignment", update=true)*/

		val assignment = newDeepAssignment()
		assignment.id = "12345"
		val submissions = for (i <- 1 to 10) yield {
			val s = new Submission
			s.assignment = assignment
			s
		}
		
		val cmd = new SubmitToTurnitinCommand(assignment, submissions)
		cmd.api = api
		cmd.apply()
	
	} finally {
		deleteTemporaryDirs
		api.destroy()
	}
	
}