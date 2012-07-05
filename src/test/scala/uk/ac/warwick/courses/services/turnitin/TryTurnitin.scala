package uk.ac.warwick.courses.services.turnitin

import collection.JavaConversions._
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
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.data.model.FileAttachment

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
		Seq("turnitin.key", "turnitin.aid", "turnitin.said").foreach { key =>
			if (!props.containsKey(key)) throw new RuntimeException("Config property " + key + " not set in local.properties")
		}
		
		logger.debug("Creating instance")
		
		api.sharedSecretKey = props.getProperty("turnitin.key")
		api.aid = props.getProperty("turnitin.aid")
		api.said = props.getProperty("turnitin.said")
		api.diagnostic = false
		
		val f = new File("src/test/resources/turnitin-submission.doc")
		if (!f.exists) throw new IllegalArgumentException("Whoops, test file doesn't exist")
		
//		api.createClass("TestClass28Jun2012")
		
//		val a = api.createAssignment("TestClass28Jun2012", "TestAssignment28Jun2012")
//		println(a)
//		
//		val b = api.createAssignment("TestClass28Jun2012", "TestAssignment28Jun2012", true)
//		println(b)
		
//		api.submitPaper("TestClass28Jun2012", "TestAssignment28Jun2012", "Paper", f, "XXXXX", "XXXXX")
//		
		val subs = api.listSubmissions("CourseworkSubmissionAPIClass", "7804b3c1-1c29-4ff1-9437-aaba07cb0954")
		println(subs)
		
//		subs match {
//			case GotSubmissions(list) => {
//				println("Deleting submissions")
//				for (item <- list) api.deleteSubmission("TestClass28Jun2012", "TestAssignment28Jun2012", item.objectId)
//			}
//				
//		}
//		
//		
//		println(api.listSubmissions("TestClass28Jun2012", "TestAssignment28Jun2012"))
		
//		val assignment = newDeepAssignment()
//		assignment.id = "12345"
//		val submissions = for (i <- 1 to 10) yield {
//			val s = new Submission
//			s.assignment = assignment
//			s.universityId = "%07d" format (123000 + i)
//			s.userId = "abcdef"
//			val attachment1 = new FileAttachment()
//			attachment1.name = "file.doc"
//			attachment1.file = f
//			val attachments = Set(attachment1)
//			s.values.add(SavedSubmissionValue.withAttachments(s, "", attachments))
//			s
//		}
		
//		val cmd = new SubmitToTurnitinCommand(assignment, submissions)
//		cmd.api = api
//		cmd.apply()
	
	} finally {
		deleteTemporaryDirs
		api.destroy()
	}
	
}