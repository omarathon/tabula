package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.{AppContextTestBase, TestBase, RequestInfo, Mockito}
import uk.ac.warwick.tabula.events.EventHandling
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.UserLookupService
import org.springframework.transaction.annotation.Transactional

class AddMarksCommandTest extends AppContextTestBase with Mockito {

	EventHandling.enabled = false
	
	/**
	 * Check that validation marks an empty mark as an invalid row
	 * so that the apply method skips it.
	 */

	@Transactional @Test
	def emptyMarkField {
		withUser("cusebr") {
			val currentUser = RequestInfo.fromThread.get.user
			val assignment = newDeepAssignment()
			val command = new AdminAddMarksCommand(assignment.module, assignment, currentUser)
			command.userLookup = mock[UserLookupService]
			command.userLookup.getUserByWarwickUniId("0672088") answers { id => 
				currentUser.apparentUser
			}
			
			val errors = new BindException(command, "command")
			
			val marks1 = command.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = ""
			
			val marks2 = command.marks.get(1)
			marks2.universityId = "1235"
			marks2.actualMark = "5"
			
			command.postExtractValidation(errors)
			command.apply()
		}
	}
	
		/**
	 * Check that validation allows either mark or grade to be non-empty
	 */
		@Transactional @Test
	def gradeButEmptyMarkField {
		withUser("cusebr") {
			val currentUser = RequestInfo.fromThread.get.user
			val assignment = newDeepAssignment()
			val command = new AdminAddMarksCommand(assignment.module, assignment, currentUser)
			command.userLookup = mock[UserLookupService]
			command.userLookup.getUserByWarwickUniId("0672088") answers { id => 
				currentUser.apparentUser
			}
			
			val errors = new BindException(command, "command")
			
			val marks1 = command.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = ""
			marks1.actualGrade = "EXCELLENT"
			
			val marks2 = command.marks.get(1)
			marks2.universityId = "1235"
			marks2.actualMark = "5"
			
			command.postExtractValidation(errors)
		
			try {
				command.apply()
				fail ("Expect to throw a NullPointerException")
			}
			 catch {
			 	case _NullPointerException => None 
			}
		}
	}
	
}