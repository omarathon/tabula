package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class ListSubmissionsResult (
	fromDate: DateTime,
	toDate: DateTime,
	submissions: Seq[Submission]
)

object ListMemberSubmissionsCommand {
	type CommandType = Appliable[ListSubmissionsResult] with ListMemberSubmissionsRequest

	def apply(member: Member, fromDate: LocalDate, toDate: LocalDate): CommandType =
		new ListMemberSubmissionsCommandInternal(member, fromDate, toDate)
			with ComposableCommand[ListSubmissionsResult]
		  with ListMemberSubmissionsRequest
			with ListMemberSubmissionsPermissions
			with AutowiringAssessmentServiceComponent
		  with ListMemberSubmissionsValidation
			with Unaudited with ReadOnly
}

trait ListMemberSubmissionsState {
	def member: Member
}

trait ListMemberSubmissionsRequest extends ListMemberSubmissionsState {
	def fromDate: LocalDate
	def toDate: LocalDate
}

trait ListMemberSubmissionsValidation extends SelfValidating {
	self: ListMemberSubmissionsRequest =>

	override def validate(errors: Errors): Unit = {
		Option(fromDate) match {
			case Some(fromDate: LocalDate) =>
				if (Option(toDate).isEmpty)
					errors.reject( "listSubmissions.api.noToDate")
				else if (fromDate.isAfter(toDate))
					errors.reject("listSubmissions.api.fromDateAfterToDate")
			case _ =>
				if (Option(toDate).isDefined)
					errors.reject("listSubmissions.api.noFromDate")
		}
	}
}

trait ListMemberSubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListMemberSubmissionsState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Submission.Read, member)
		p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, member)
		p.PermissionCheck(Permissions.Extension.Read, member)
		p.PermissionCheck(Permissions.Module.ManageAssignments, member)
	}
}

abstract class ListMemberSubmissionsCommandInternal(val member: Member, val fromDate: LocalDate, val toDate: LocalDate)
	extends CommandInternal[ListSubmissionsResult] with ListMemberSubmissionsState with TaskBenchmarking {

		self: AssessmentServiceComponent
		with ListMemberSubmissionsRequest
		with ListMemberSubmissionsPermissions =>

	override def applyInternal(): ListSubmissionsResult = {

		val submissionsFromDate = Option(fromDate).map(_.toDateTimeAtStartOfDay).getOrElse(
			DateTime.now.minusYears(1).withTimeAtStartOfDay
		)
		val submissionsToDate = Option(toDate).map(_.toDateTimeAtStartOfDay).getOrElse(
			DateTime.now.withTimeAtStartOfDay
		)

		val submissions = assessmentService.getSubmissionsForAssignmentsBetweenDates(member.userId, submissionsFromDate, submissionsToDate)

		ListSubmissionsResult (
			fromDate = submissionsFromDate,
			toDate = submissionsToDate,
			submissions = submissions
		)
	}
}