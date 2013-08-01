package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import org.joda.time.DateTime

class CopyAssignmentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AssignmentServiceComponent {
		val assignmentService = mock[AssignmentService]
		def apply(): Seq[Assignment] = Seq()
	}

	trait Fixture {
		val module = new Module("BS101")
		val fakeDate = new DateTime(2012, 8, 23, 0, 0)

		val assignment = new Assignment
		assignment.academicYear = AcademicYear.parse("12/13")
		assignment.module = module
		assignment.name = "Test"
		assignment.openDate = fakeDate
		assignment.closeDate = fakeDate.plusDays(30)
		assignment.openEnded = false
		assignment.collectMarks = true
		assignment.collectSubmissions = true
		assignment.restrictSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = false
		assignment.displayPlagiarismNotice = true
		assignment.allowExtensions = true
		assignment.allowExtensionRequests = false
		assignment.summative = false
	}

	@Test
	def commandApply() {
		new Fixture {
			withFakeTime(fakeDate) {
				val command = new CopyAssignmentsCommand with CommandTestSupport
				command.assignments = Seq(assignment)
				command.archive = true
				val newAssignment = command.applyInternal().get(0)

				there was one(command.assignmentService).save(assignment)
				there was one(command.assignmentService).save(newAssignment)

				assignment.archived.booleanValue should be(true)
				newAssignment.academicYear.toString should be("12/13")
				newAssignment.module should be(module)
				newAssignment.name should be("Test")
				newAssignment.openDate should be(fakeDate)
				newAssignment.closeDate should be(fakeDate.plusDays(30))
				newAssignment.openEnded.booleanValue should be(false)
				newAssignment.collectMarks.booleanValue should be(true)
				newAssignment.collectSubmissions.booleanValue should be(true)
				newAssignment.restrictSubmissions.booleanValue should be(true)
				newAssignment.allowLateSubmissions.booleanValue should be(true)
				newAssignment.allowResubmission.booleanValue should be(false)
				newAssignment.displayPlagiarismNotice.booleanValue should be(true)
				newAssignment.allowExtensions.booleanValue should be(true)
				newAssignment.allowExtensionRequests.booleanValue should be(false)
				newAssignment.summative.booleanValue should be(false)
		}
		}
	}

}
