package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ReleaseExamForMarkingCommand {
	def apply(module: Module, exam: Exam, currentUser: CurrentUser) =
		new ReleaseExamForMarkingCommandInternal(module, exam, currentUser)
			with ComposableCommand[Exam]
			with AutowiringAssessmentServiceComponent
			with ReleaseExamForMarkingCommandDescription
			with ReleaseExamForMarkingCommandPermissions
			with ReleaseExamForMarkingCommandState
}


class ReleaseExamForMarkingCommandInternal(val module: Module, val exam: Exam, currentUser: CurrentUser)
	extends CommandInternal[Exam] {

	self: AssessmentServiceComponent =>

	override def applyInternal() = {
		exam.released = true
		assessmentService.save(exam)
		exam
	}

}

trait ReleaseExamForMarkingCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReleaseExamForMarkingCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(exam), mandatory(module))
		p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
	}

}

trait ReleaseExamForMarkingCommandDescription extends Describable[Exam] {

	self: ReleaseExamForMarkingCommandState =>

	override lazy val eventName = "ReleaseExamForMarking"

	override def describe(d: Description) {
		d.exam(exam)
	}
}

trait ReleaseExamForMarkingCommandState {
	def module: Module
	def exam: Exam
}
