package uk.ac.warwick.tabula.coursework.commands
import uk.ac.warwick.tabula.coursework.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.tabula.coursework.commands.assignments.AddAssignmentCommand
import uk.ac.warwick.tabula.coursework.services.ModuleAndDepartmentService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.ModuleDao
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.data.model.forms._
import uk.ac.warwick.tabula.coursework.data.model._

class AddAssignmentCommandContextTest extends AppContextTestBase {
	
	@Autowired var modules:ModuleDao =_
	
	@Transactional
	@Test def edit() {
		withUser("abc") {
			
		}
	}
	
	@Transactional
	@Test def add() {

		withUser("abc") {
			
			val dept = new Department
			dept.code = "in"
			session.save(dept)
		
			val module = new Module
			module.department = dept
			modules.saveOrUpdate(module)
			session.flush() // get out of my car, get into my database
			
			module.id should not be (null)
			
			val command = new AddAssignmentCommand(module)
			
			command.name = "Assignment name"
			command.comment = "Text at the top"
			val assignmentNew = command.apply
			
			session.flush()
			session.clear()
			
			val assignment = session.get(classOf[Assignment], assignmentNew.id).asInstanceOf[Assignment]
			
			assignment.fields.size should be (2)
			assignment.fields.get(0) should have('class(classOf[CommentField]))
			assignment.fields.get(0).template should be ("comment")
			assignment.fields.get(0).propertiesMap("value") should be("Text at the top")
			assignment.fields.get(1) should have('class(classOf[FileField]))
			assignment.fields.get(1).assignment should be(assignment)
			assignment.fields.get(1).position should be(1)
			assignment.fields.get(1).template should be("file")
		}
	}

  @Test def massAddUsers {
    val form = new AddAssignmentCommand()
    form.massAddUsers =
      """ cusebr
          cusfal
          ecu
          0123456
          whatever yep good
      """
    form.massAddUsersEntries should be (Seq("cusebr","cusfal","ecu","0123456","whatever","yep","good"))
  }
}