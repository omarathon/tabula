package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{UpstreamAssessmentGroup, AssessmentGroup, Assignment}
import org.joda.time.DateTime

class CopyAssignmentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AssignmentServiceComponent with AssignmentMembershipServiceComponent {
		val assignmentService = mock[AssignmentService]
		val assignmentMembershipService = mock[AssessmentMembershipService]
		def apply(): Seq[Assignment] = Seq()
	}

	trait Fixture {
		val department = Fixtures.department("bs")
		val module = Fixtures.module("bs101")
		module.adminDepartment = department
		
		val fakeDate = new DateTime(2013, 8, 23, 0, 0)

		val assignment = Fixtures.assignment("Test")
		assignment.addDefaultFields()
		assignment.academicYear = AcademicYear.parse("12/13")
		assignment.module = module
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
		assignment.summative = false
	}

	@Test
	def commandApply() {
		new Fixture {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
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
				val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
				command.assignments = Seq(assignment)
				command.archive = true
				val newAssignment = command.applyInternal().get(0)

				assignment.archived.booleanValue should be(true)
				newAssignment.academicYear.toString should be("13/14")
				newAssignment.module should be(module)
				newAssignment.name should be("Test")
				newAssignment.openDate should be(new DateTime(2014, 8, 22, 0, 0))
				newAssignment.closeDate should be(new DateTime(2014, 8, 22, 0, 0).plusDays(30))
				newAssignment.openEnded.booleanValue should be(false)
				newAssignment.collectMarks.booleanValue should be(true)
				newAssignment.collectSubmissions.booleanValue should be(true)
				newAssignment.restrictSubmissions.booleanValue should be(true)
				newAssignment.allowLateSubmissions.booleanValue should be(true)
				newAssignment.allowResubmission.booleanValue should be(false)
				newAssignment.displayPlagiarismNotice.booleanValue should be(true)
				newAssignment.allowExtensions.booleanValue should be(true)
				newAssignment.summative.booleanValue should be(false)
			}
		}
	}

	@Test def guessSitsLinks() {
		new Fixture {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
			command.assignments = Seq(assignment)
			command.academicYear = AcademicYear.parse("13/14")

			val ag1 = {
				val group = new AssessmentGroup
				group.assignment = assignment
				group.occurrence = "A"
				group.assessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("bs101"), 1)
				group.membershipService = command.assignmentMembershipService
				group
			}

			val ag2 = {
				val group = new AssessmentGroup
				group.assignment = assignment
				group.occurrence = "B"
				group.assessmentComponent = Fixtures.upstreamAssignment(Fixtures.module("bs102"), 2)
				group.membershipService = command.assignmentMembershipService
				group
			}

			assignment.assessmentGroups.add(ag1)
			assignment.assessmentGroups.add(ag2)

			val template1 = {
				val template = new UpstreamAssessmentGroup
				template.academicYear = AcademicYear.parse("13/14")
				template.assessmentGroup = ag1.assessmentComponent.assessmentGroup
				template.moduleCode = ag1.assessmentComponent.moduleCode
				template.occurrence = ag1.occurrence
				template
			}
			val template2 = {
				val template = new UpstreamAssessmentGroup
				template.academicYear = AcademicYear.parse("13/14")
				template.assessmentGroup = ag2.assessmentComponent.assessmentGroup
				template.moduleCode = ag2.assessmentComponent.moduleCode
				template.occurrence = ag2.occurrence
				template
			}

			command.assignmentMembershipService.getUpstreamAssessmentGroup(any[UpstreamAssessmentGroup]) answers { t =>
				val template = t.asInstanceOf[UpstreamAssessmentGroup]
				if (template.occurrence == "A")
					Some(Fixtures.assessmentGroup(template1.academicYear, ag1.assessmentComponent.assessmentGroup, ag1.assessmentComponent.moduleCode, ag1.occurrence))
				else
					None
			}

			val newAssignment = command.applyInternal().get(0)
			newAssignment.assessmentGroups.size should be (1)

			val link = newAssignment.assessmentGroups.get(0)
			link.assessmentComponent should be (ag1.assessmentComponent)
			link.assignment should be (newAssignment)
			link.occurrence should be (ag1.occurrence)
		}
	}

	@Test
	def copyDefaultFields() {
		new Fixture with FindAssignmentFields {
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
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
			val command = new CopyAssignmentsCommand(department, Seq(module)) with CommandTestSupport
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
