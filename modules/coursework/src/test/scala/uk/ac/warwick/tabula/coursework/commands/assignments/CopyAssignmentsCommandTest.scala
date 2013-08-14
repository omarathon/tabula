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
		val fakeDate = new DateTime(2013, 8, 23, 0, 0)

		val assignment = new Assignment
		assignment.addDefaultSubmissionFields()
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
			val command = new CopyAssignmentsCommand(Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			command.archive = true
			val newAssignment = command.applyInternal().get(0)

			there was one(command.assignmentService).save(assignment)
			there was one(command.assignmentService).save(newAssignment)
		}
	}

	@Test
	def copy() {
		new Fixture with FindAssignmentFields {
			withFakeTime(fakeDate) {
				val command = new CopyAssignmentsCommand(Seq(module)) with CommandTestSupport
				command.assignments = Seq(assignment)
				command.archive = true
				val newAssignment = command.applyInternal().get(0)

				assignment.archived.booleanValue should be(true)
				newAssignment.academicYear.toString should be("13/14")
				newAssignment.module should be(module)
				newAssignment.name should be("Test")
				newAssignment.openDate should be(fakeDate.plusYears(1))
				newAssignment.closeDate should be(fakeDate.plusDays(30).plusYears(1))
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

	@Test
	def copyDefaultFields() {
		new Fixture with FindAssignmentFields {
			val command = new CopyAssignmentsCommand(Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			val newAssignment = command.applyInternal().get(0)

			findCommentField(newAssignment).get.value should be ("")
			findFileField(newAssignment).get.attachmentLimit should be (1)
			findFileField(newAssignment).get.attachmentTypes should be (Nil)
			findWordCountField(newAssignment).max should be(null)
			findWordCountField(newAssignment).min should be(null)
			findWordCountField(newAssignment).conventions should be(null)
		}
	}

	@Test
	def copyFieldValues() {
		new Fixture with FindAssignmentFields {

			val heronRant = "Words describing the evil nature of Herons will not count towards the final word count. Herons are scum. Hate them!"
			findWordCountField(assignment).max = 5000
			findWordCountField(assignment).min = 4500
			findWordCountField(assignment).conventions = heronRant
			val extremeHeronRant = heronRant.replace("Hate them", "Spit at them!")
			findCommentField(assignment).get.value = extremeHeronRant
			findFileField(assignment).get.attachmentLimit = 9999
			findFileField(assignment).get.attachmentTypes = Seq(".hateherons")
			val command = new CopyAssignmentsCommand(Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			val newAssignment = command.applyInternal().get(0)

			findCommentField(newAssignment).get.value should be (extremeHeronRant)
			findFileField(newAssignment).get.attachmentLimit should be (9999)
			findFileField(newAssignment).get.attachmentTypes should be (Seq(".hateherons"))
			findWordCountField(newAssignment).max should be(5000)
			findWordCountField(newAssignment).min should be(4500)
			findWordCountField(newAssignment).conventions should be(heronRant)
		}
	}

}
