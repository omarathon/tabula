package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.Mockito
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.forms.Extension
import collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.SendSubmissionNotifyCommand
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Before

// scalastyle:off magic.number
class SendSubmissionNotifyCommandTest extends AppContextTestBase with Mockito {

	var submission: Submission = _
	val u: User = newTestUser
	var ug = new UserGroup()
	var userSettings = new UserSettings()
	var sc: SendSubmissionNotifyCommand = _
	val userLookup = mock[UserLookupService]
	when(userLookup.getUserByUserId("test")).thenReturn(u)
	
	@Before def before {
		ug.includeUsers = List ("test")
		submission = newBaseSubmission
		sc = { 
			val sendCmd = new SendSubmissionNotifyCommand(submission, ug)
			sendCmd.userLookup = userLookup
			sendCmd.userSettings = mock[UserSettingsService]
			sendCmd
		}
	}
	
	
	@Test def allSubmissions {
		userSettings.settings = Map("alertsSubmission" -> "allSubmissions")
		when(sc.userSettings.getByUserId("test")).thenReturn(Option(userSettings))

		sc.applyInternal()
		val notification = sc.emit.get(0) // should only be one so get it!
		notification.recipients.size should be(1)
		
		val text = notification.content
		text should include (submission.assignment.module.name)
		text should include (submission.id)
	}
	
	
	@Test def noAlerts {
		userSettings.settings = Map("alertsSubmission" -> "none")
		when(sc.userSettings.getByUserId("test")).thenReturn(Option(userSettings))

		sc.applyInternal()
		sc.emit should be(Nil) // should not be any notifications
	}
	
	
	@Test def lateSubmissions {
		userSettings.settings = Map("alertsSubmission" -> "lateSubmissions")
		submission.submittedDate = new DateTime(2013, 1, 12, 12, 0)
		submission.assignment.extensions add newExtension
		when(sc.userSettings.getByUserId("test")).thenReturn(Option(userSettings))
		
		sc.applyInternal()
		val notification = sc.emit.get(0) // should only be one so get it!
		notification.recipients.size should be(1)
	}
	
	
	@Test def lateSubmissionsIgnoreOnTime {
		userSettings.settings = Map("alertsSubmission" -> "lateSubmissions")
		when(sc.userSettings.getByUserId("test")).thenReturn(Option(userSettings))
		
		sc.applyInternal()
		sc.applyInternal()
		sc.emit should be(Nil) // should not be any notifications
	}
	

	def newExtension() = {
		val extension = new Extension()
	    extension.universityId = "1171795"
	    extension.userId = "cuslat"
	    extension.expiryDate = new DateTime().plusWeeks(1)
	    extension.reason = "I lost my work down the back of the dog"
	    extension.approvalComments = "Naughty dog. OK."
	    extension.approved = true
	    extension.approvedOn = new DateTime(2012, 7, 22, 14, 42)
		extension
	}
	
	
	def newTestUser = { 
		val u = new User("test")
		u.setFoundUser(true)
		u.setWarwickId("1000000")
		u.setEmail("test@warwick.ac.uk")
		u
	}

	
	def newBaseSubmission = {
		var submission = new Submission
		submission.id = "000000001"
		submission.universityId = "1171795"
		submission.userId = "cuslat"
		submission.assignment = newBaseAssignment
		submission
	}
	
	
	def newBaseAssignment = {
		var assignment = new Assignment
	    assignment.addDefaultFields
	    assignment.module = new Module
	    assignment.module.code = "AA001"
	    assignment.module.name = "Really difficult module"
	    assignment.members = null
	    assignment.id ="0000123"
	    assignment.name = "My essay"
	    assignment.commentField.get.value = "Instructions"	
	    assignment.closeDate = 	new DateTime(2012, 7, 12, 12, 0)
	    assignment
	}

}
