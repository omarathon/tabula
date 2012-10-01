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
		//api.said = props.getProperty("turnitin.said")
		api.integrationId = "80"
		api.apiEndpoint = props.getProperty("turnitin.url")
		api.diagnostic = false

		println("Hello")
//		val nick = api.login("nickhowes@gmail.com", "Nick","Howes")
		val nick = api.login("greg.evigan@nickhowes.co.uk", "Greg","Evigan")
		println(nick)

		val testClassName = "TestClass1"
		val testAssName = "TestAss1"
			
		val f = new File("src/test/resources/turnitin-submission.doc")
		if (!f.exists) throw new IllegalArgumentException("Whoops, test file doesn't exist")

		for (session <- nick) {
			println("logged in " + session.userEmail)

			val nickid = session.getUserId()
			for (userid <- nickid) {
				println("userid is %s" format userid)
				session.userId = userid

				val addTutor = session.addTutor("4320278", testClassName)
				println(addTutor)
				
//				session.createClass(testClassName) match {
//					case Created(id) => {
//						println("Created class %s" format id)
//						val addTutor = session.addTutor(id, testClassName)
//						println(addTutor)
//						
//						val ass = session.createOrUpdateAssignment(testClassName, testAssName)
//						println("Assignment " + ass)
//						ass match {
//							case Created(assignmentId) => {
//								val submit = session.submitPaper(id, assignmentId, "Paper", f, "XXXXX", "XXXXX")
//					            println("Submission " + submit)
//							}
//						}
//						
//					}
//					case a => println(a)
//				}
			}
			
			println("Login URL")
			println(session.getLoginLink())
		
			// don't logout as this will invalidate any URLs we've generated.
			//session.logout()
		}
//		
//		val nick2 = api.login("nick@nickhowes.co.uk", "Nick","Howestwo")
//		for (session <- nick2) {
//			println("logged in nick2.")
//            
//            session.createClass(testClassName) match {
//				case Created(id) => println("Created class %s" format id)
//			}
//            
//        
//            session.logout()
//		}
		
		
	
	} finally {
		deleteTemporaryDirs
		api.destroy()
	}
	
}