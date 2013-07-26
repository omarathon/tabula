package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent, UserLookupService, SmallGroupService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.Errors
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.ProfileServiceComponent

class RecordAttendanceCommandTest extends TestBase with Mockito {
	
	val smallGroupEventOccurrence = mock[SmallGroupEventOccurrence]

	// Implements the dependencies declared by the command
	trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent {
		val smallGroupService = mock[SmallGroupService]
		val userLookup = mock[UserLookupService]
		var profileService = mock[ProfileService]
		
		def apply(): SmallGroupEventOccurrence = {
			smallGroupEventOccurrence
		}
	}

	@Test
	def commandApply() {
		val event = mock[SmallGroupEvent]
		val week = 1
		val user = new User("abcde")

		val command = new RecordAttendanceCommand(event, week) with CommandTestSupport
		command.attendees.add("abcde")
		val occurrence = command.applyInternal()

		there was no(command.userLookup).getUsersByUserIds(Seq("abcde").asJava)
		there was one(command.smallGroupService).updateAttendance(event, week, Seq("abcde"))
	}
	
	@Test
	def validate() {
		val invalidUser = new User("invalid")
		invalidUser.setFoundUser(false);
		val missingUser = new User("missing")
		missingUser.setFoundUser(true);
		val validUser = new User("valid")
		validUser.setFoundUser(true);
		val event = new SmallGroupEvent()
		val group = new SmallGroup()
		val students = new UserGroup()
		group.students = students
		event.group = group
		val week = 1

		val command = new RecordAttendanceCommand(event, week) with CommandTestSupport
		
		command.userLookup.getUserByWarwickUniId(invalidUser.getUserId()) returns (invalidUser)
		command.userLookup.getUserByWarwickUniId(missingUser.getUserId()) returns (missingUser)
		command.userLookup.getUserByWarwickUniId(validUser.getUserId()) returns (validUser)
		
		command.attendees = JArrayList()
		command.attendees.add(invalidUser.getUserId())
		command.attendees.add(validUser.getUserId())
		
		var errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasFieldErrors() should be (true)
		errors.getFieldError("attendees").getArguments() should have size (1) 
		
		command.attendees = JArrayList()
		command.attendees.add(missingUser.getUserId())
		command.attendees.add(validUser.getUserId())
		
		errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasFieldErrors() should be (true)
		errors.getFieldError("attendees").getArguments() should have size (1) 
	}

}
