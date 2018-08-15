package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class ListSubmissionsResult(
	fromDate: DateTime,
	toDate: DateTime,
	academicYear: AcademicYear,
	submissions: Seq[Submission]
)

case class ViewSubmission (
	submission: Submission,
	assignment: Assignment,
	module: Module,
	extension: Option[Extension]
)

object SubmissionsCommand {
	type CommandType = Appliable[ListSubmissionsResult] with SubmissionsRequest

	def apply(department: Department, member: Member) =
		new SubmissionsCommandInternal(department, member)
			with ComposableCommand[ListSubmissionsResult]
		  with SubmissionsRequest
			with SubmissionsPermissions
			with AutowiringAssessmentServiceComponent
			with Unaudited with ReadOnly

}

trait SubmissionsState {
	def department: Department
	def member: Member
}

trait SubmissionsRequest extends SubmissionsState {
	var academicYear: AcademicYear = AcademicYear(2017)
}

//trait SubmissionsValidation extends SelfValidating {
//
//	override def validate(errors: Errors): Unit = {
//	}
//}

trait SubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmissionsState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		//mustBeLinked(notDeleted(mandatory(assignment)), mandatory(module))
		p.PermissionCheck(Permissions.Submission.Read, department)
		p.PermissionCheck(Permissions.Submission.Read, member)
		p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, department)
		p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, member)
	}
}

abstract class SubmissionsCommandInternal(val department: Department, val member: Member)
	extends CommandInternal[ListSubmissionsResult] with SubmissionsState with TaskBenchmarking {

		self: AssessmentServiceComponent
		with SubmissionsRequest
		with SubmissionsPermissions =>

	override def applyInternal(): ListSubmissionsResult = {

		val submissions = assessmentService.getSubmissionsForAssignmentsBetweenDates(member.userId, academicYear.firstDay.toDateTimeAtStartOfDay, academicYear.lastDay.toDateTimeAtStartOfDay)

		ListSubmissionsResult (
			fromDate = academicYear.firstDay.toDateTimeAtStartOfDay,
			toDate = academicYear.lastDay.toDateTimeAtStartOfDay,
			academicYear: AcademicYear,
			submissions = submissions
		)
	}
}