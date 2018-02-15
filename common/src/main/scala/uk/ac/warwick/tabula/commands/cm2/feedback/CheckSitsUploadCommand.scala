package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.CheckSitsUploadCommand.Result
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringFeedbackForSitsServiceComponent
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsService, ExportFeedbackToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CheckSitsUploadCommand {
	case class Result(
		hasAssessmentGroups: Boolean = false,
		hasMultipleAssessmentGroups: Boolean = false,
		hasAssignmentRow: Boolean = false,
		hasWritableMark: Boolean = false,
		hasWritableGrade: Boolean = false
	)
	type Command = Appliable[Result]

	def apply(assignment: Assignment, feedback: Feedback): Command =
		new CheckSitsUploadCommandInternal(assignment, feedback)
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringExportFeedbackToSitsServiceComponent
			with ComposableCommand[CheckSitsUploadCommand.Result]
			with CheckSitsUploadPermissions
			with CheckSitsUploadCommandState
			with ReadOnly with Unaudited
}

class CheckSitsUploadCommandInternal(val assignment: Assignment, val feedback: Feedback) extends CommandInternal[CheckSitsUploadCommand.Result] {
	self: ExportFeedbackToSitsServiceComponent with CheckSitsUploadCommandState =>

	override def applyInternal(): Result = {
		if (feedback.assessmentGroups.isEmpty) {
			CheckSitsUploadCommand.Result(hasAssessmentGroups = false)
		} else if (feedback.assessmentGroups.size > 1) {
			CheckSitsUploadCommand.Result(
				hasAssessmentGroups = true,
				hasMultipleAssessmentGroups = true
			)
		} else {
			exportFeedbackToSitsService.getPartialMatchingSasRecords(feedback) match {
				case Seq() =>
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = false
					)
				case Seq(singleRow) =>
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = true,
						hasWritableMark = singleRow.actualMark.isEmpty || singleRow.uploader == ExportFeedbackToSitsService.tabulaIdentifier,
						hasWritableGrade = singleRow.actualGrade == null || singleRow.uploader == ExportFeedbackToSitsService.tabulaIdentifier
					)
				case rows =>
					val hasWritableMarkAndGrade = rows.exists(row =>
						(row.actualMark.isEmpty || row.uploader == ExportFeedbackToSitsService.tabulaIdentifier)
							&& (row.actualGrade == null || row.uploader == ExportFeedbackToSitsService.tabulaIdentifier)
					)
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = true,
						hasWritableMark = hasWritableMarkAndGrade,
						hasWritableGrade = hasWritableMarkAndGrade
					)
			}
		}
	}
}

trait CheckSitsUploadPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CheckSitsUploadCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		HibernateHelpers.initialiseAndUnproxy(feedback) match {
			case assignmentFeedback: AssignmentFeedback =>
				mustBeLinked(mandatory(assignmentFeedback), mandatory(assignment))
				p.PermissionCheck(Permissions.AssignmentFeedback.Publish, assignmentFeedback)
			case examFeedback: ExamFeedback =>
				p.PermissionCheck(Permissions.ExamFeedback.Manage, examFeedback)
		}
	}
}

trait CheckSitsUploadCommandState {
	def assignment: Assignment
	def feedback: Feedback
}
