package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList}

object EditExamCommand {
	def apply(exam: Exam) =
		new EditExamCommandInternal(exam)
			with ComposableCommand[Exam]
			with EditExamPermissions
			with EditExamCommandState
			with EditExamCommandDescription
			with PopulateEditExamCommand
			with ExamValidation
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with UpdatesStudentMembership
			with ModifiesExamMembership
			with HasAcademicYear
			with AutowiringUserLookupComponent
			with SpecifiesGroupType
}

class EditExamCommandInternal(override val exam: Exam)
	extends CommandInternal[Exam]
	with EditExamCommandState
	with UpdatesStudentMembership
	with ModifiesExamMembership {

	self: AssessmentServiceComponent with UserLookupComponent  with HasAcademicYear with SpecifiesGroupType
		with AssessmentMembershipServiceComponent =>

	override def applyInternal() = {
		exam.name = name

		exam.assessmentGroups.clear()
		exam.assessmentGroups.addAll(assessmentGroups)
		for (group <- exam.assessmentGroups if group.exam == null) {
			group.exam = exam
		}

		assessmentService.save(exam)
		exam
	}
}

trait EditExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, exam)
	}
}

trait EditExamCommandState extends ExamState {

	def exam: Exam
	override def module: Module = exam.module
	override def academicYear: AcademicYear = exam.academicYear
}

trait EditExamCommandDescription extends Describable[Exam] {

	self: EditExamCommandState =>

	def describe(d: Description) {
		d.exam(exam)
	}
}

trait PopulateEditExamCommand {

	self: EditExamCommandState with UpdatesStudentMembership =>
	name = exam.name
	assessmentGroups = exam.assessmentGroups

	def populateGroups(exam: Exam) {
		assessmentGroups = exam.assessmentGroups
		upstreamGroups.addAll(availableUpstreamGroups filter { ug =>
			assessmentGroups.exists( ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence )
		})
	}

}