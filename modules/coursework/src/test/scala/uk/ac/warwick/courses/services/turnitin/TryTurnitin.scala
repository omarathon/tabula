package uk.ac.warwick.courses.services.turnitin

import collection.JavaConversions._
import org.apache.commons.codec.digest.DigestUtils
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.TestHelpers
import java.io.FileWriter
import uk.ac.warwick.courses.commands.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.courses.TestFixtures
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.data.model.FileAttachment
import uk.ac.warwick.util.web.UriBuilder

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
		//api.said = props.getProperty("turnitin.said")
		api.integrationId = "80"
		api.apiEndpoint = props.getProperty("turnitin.url")
		api.diagnostic = false

		println("Hello")
		val nick = api.login("nick@nickhowes.co.uk", "Nick","Howes")
		
		val ritchie = api.login("ritchie.allen@warwick.ac.uk", "Ritchie", "Allllen")
		
		println(ritchie)
		

		val testClassName = ClassName("TestClass1")
		val testAssName = AssignmentName("TestAss1")
		// think we just have to store these and know them.
        val classId = ClassId("ExtNewClassId")
        val assId = AssignmentId("ExtNewAssId")
			
		val f = new File("src/test/resources/turnitin-submission.doc")
		if (!f.exists) throw new IllegalArgumentException("Whoops, test file doesn't exist")

		for (session <- nick) {
			println("logged in " + session.userEmail)

			println("userid is %s" format session.userId)
			
//			println(session.createClass(classId, ClassName(classId.value + " Name")))
//			println(session.createAssignment(classId, testClassName, assId, testAssName))

			//			val addTutor = session.addTutor(ClassId("KevinBacon"), ClassName("Roger Moore"))
			//			println("add Tutor: " + addTutor)

			//			println(session.createAssignment(classId, testAssName))

			val submit = session.submitPaper(classId, testClassName, assId, testAssName, "Cool paper", "coolpaper.doc", f, "Johnny", "Badessay")

			val submissions = session.listSubmissions(classId, assId)
			println(submissions)
			submissions match {
				case GotSubmissions(list) => for (submission <- list) {
					println(submission)
					println("REPORT: " + session.getReport( submission.objectId ))
//					println("Doc Viewer: " + session.getDocumentViewerLink( submission.objectId ))
				}
				case other => println("Oh no some problem getting submissions :( " + other)
			}
//
			println("Login URL")
			println(session.getLoginLink().get)

			// don't logout as this will invalidate any URLs we've generated.
			//session.logout()
		}
		
	
	} finally {
		deleteTemporaryDirs
		api.destroy()
	}
	
}
