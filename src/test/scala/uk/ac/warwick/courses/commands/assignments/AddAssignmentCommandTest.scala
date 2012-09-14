package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.data.model.Department
import org.springframework.validation.BindException
import uk.ac.warwick.courses.helpers.ArrayList
import org.joda.time.DateTime
import collection.JavaConversions._

class AddAssignmentCommandTest extends TestBase {

	@Test def validation {
		val department = new Department
		department.code = "ch"

		val cmd = new AddAssignmentsCommand(department);

		{
			cmd.assignmentItems = ArrayList(
				new AssignmentItem {
					name = "Johnny"
					optionsId = "A"
				})
			val errors = new BindException(cmd, "command");
			cmd.validate(errors)
			errors.hasErrors should be(true)
		}

		{
			cmd.assignmentItems = ArrayList(
				new AssignmentItem {
					name = "Johnny"
					optionsId = "A"
					openDate = new DateTime()
					closeDate = new DateTime().plusWeeks(10)
				})
			cmd.optionsMap = Map(
			    "A" -> new SharedAssignmentPropertiesForm {
			    	collectMarks = true
			    	collectSubmissions = true
			    	restrictSubmissions = true
			    	allowLateSubmissions = true
			    	displayPlagiarismNotice = true
			    }
			)
			val errors = new BindException(cmd, "command");
			cmd.validate(errors)
			errors.hasErrors should be(false)
		}
	}

}