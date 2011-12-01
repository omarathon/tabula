package uk.ac.warwick.courses.commands
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.courses.commands.assignments.AddAssignmentCommand
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.ModuleDao
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.courses.data.model.forms._

class AddAssignmentCommandTest extends AppContextTestBase {
	
	@Autowired var modules:ModuleDao =_
	
	@Transactional
	@Test def add {
		withUser("abc") {
		
			val module = new Module
			modules.saveOrUpdate(module)
			val command = new AddAssignmentCommand(module)
			
			command.name = "Assignment name"
			val assignment = command.apply
			
			session.flush
			
			assignment.fields.size should be (2)
			assignment.fields.get(0) should have('class(classOf[CommentField]))
			assignment.fields.get(1) should have('class(classOf[FileField]))
		
		}
	}
}