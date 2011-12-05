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
import uk.ac.warwick.courses.data.model.Assignment

class AddAssignmentCommandTest extends AppContextTestBase {
	
	@Autowired var modules:ModuleDao =_
	
	@Transactional
	@Test def add {

		withUser("abc") {
		
			val module = new Module
			modules.saveOrUpdate(module)
			session.flush // get out of my car, get into my database
			
			module.id should not be (null)
			
			val command = new AddAssignmentCommand(module)
			
			command.name = "Assignment name"
			val assignmentNew = command.apply
			
			session.flush
			session.clear
			
			val assignment = session.get(classOf[Assignment], assignmentNew.id).asInstanceOf[Assignment]
			
			assignment.fields.size should be (2)
			assignment.fields.get(0) should have('class(classOf[CommentField]))
			assignment.fields.get(0).template should be ("comment")
			assignment.fields.get(0).propertiesMap("value") should be("")
			assignment.fields.get(1) should have('class(classOf[FileField]))
			assignment.fields.get(1).assignment should be(assignment)
			assignment.fields.get(1).position should be(1)
			assignment.fields.get(1).template should be("file")
		
//			val q = session.createSQLQuery("select position from formfield where fieldtype=:d")
//			q.setString("d", "upload")
//			val position = q.uniqueResult()
//			position should be (1)
		}
	}
}