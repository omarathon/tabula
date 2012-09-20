package uk.ac.warwick.courses.commands.assignments

import scala.collection.JavaConversions._

import org.junit.Test
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindException

import uk.ac.warwick.courses.AppContextTestBase
import uk.ac.warwick.courses.Fixtures
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.data.model._

class AddAssignmentsCommandTest extends AppContextTestBase {
	
	@Transactional
	@Test def applyCommand {
		val f = MyFixtures()
		
		val cmd = new AddAssignmentsCommand(f.department)
		cmd.academicYear = new AcademicYear(2012)
		cmd.assignmentItems = Seq(
			item(f.upstream1, true, "A"),
			item(f.upstream2, false, null)
		)
		cmd.optionsMap = Map(
			"A" -> new SharedAssignmentPropertiesForm
		)
		
		// check validation
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (false)
		
		cmd.apply
		
		val query = session.createQuery("from Assignment where module=:module")
		query.setEntity("module", f.module)
		val result = query.uniqueResult().asInstanceOf[Assignment]
		result.name should be ("Assignment 1")
	} 
	
	
	case class MyFixtures() {
		val department = Fixtures.department(code="ls", name="Life Sciences")
        val upstream1 = Fixtures.upstreamAssignment(departmentCode="ls", number=1)
        val upstream2 = Fixtures.upstreamAssignment(departmentCode="ls", number=2)
        val assessmentGroup1 = Fixtures.assessmentGroup(upstream1)
        val module = Fixtures.module(code="ls101")
        
        session.save(department)
        session.save(upstream1)
        session.save(upstream2)
        session.save(assessmentGroup1)
        session.save(module)
	}
	
	def item(assignment: UpstreamAssignment, include: Boolean, optionsId: String) = {
		val item = new AssignmentItem(include, "A", assignment)
		item.optionsId = optionsId
		item.openDate  = dateTime(2012, 9)
		item.closeDate = dateTime(2012, 11)
		item
	}
	

}