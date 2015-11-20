package uk.ac.warwick.tabula.commands.coursework.turnitin

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent, CurrentUser}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.coursework.SubmitToTurnitinJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.jobs.{JobServiceComponent, AutowiringJobServiceComponent, JobInstance}
import uk.ac.warwick.tabula.jobs.coursework.SubmitToTurnitinLtiJob
import org.springframework.validation.Errors
import uk.ac.warwick.userlookup.User

/**
	* Creates a job that submits the assignment to Turnitin.
	*
	* Returns the job instance ID for status tracking.
	*/
object SubmitToTurnitinCommand {
	type CommandType = Appliable[JobInstance] with SubmitToTurnitinRequest with SelfValidating

	def apply(module: Module, assignment: Assignment): CommandType =
		new SubmitToTurnitinCommandInternal(module, assignment)
			with ComposableCommand[JobInstance]
			with SubmitToTurnitinPermissions
			with SubmitToTurnitinDescription
			with SubmitToTurnitinValidation
			with AutowiringJobServiceComponent
			with AutowiringFeaturesComponent

	def apply(module: Module, assignment: Assignment, user: CurrentUser): CommandType =
		new SubmitToTurnitinCommandInternal(module, assignment, user)
			with ComposableCommand[JobInstance]
			with SubmitToTurnitinPermissions
			with SubmitToTurnitinDescription
			with SubmitToTurnitinValidation
			with AutowiringJobServiceComponent
			with AutowiringFeaturesComponent
}

trait SubmitToTurnitinState {
	def module: Module
	def assignment: Assignment
}

trait SubmitToTurnitinRequest extends SubmitToTurnitinState {
	var submitter: User = _
}

abstract class SubmitToTurnitinCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[JobInstance]
		with SubmitToTurnitinRequest {
	self: JobServiceComponent with FeaturesComponent =>

	def this(module: Module, assignment: Assignment, user: CurrentUser) {
		this(module, assignment)

		submitter = user.apparentUser
	}

	override def applyInternal() = {
		if (features.turnitinLTI) jobService.add(submitter, SubmitToTurnitinLtiJob(assignment))
		else jobService.add(submitter, SubmitToTurnitinJob(assignment))
	}

}

trait SubmitToTurnitinPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmitToTurnitinState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Submission.CheckForPlagiarism, assignment)
	}
}

trait SubmitToTurnitinDescription extends Describable[JobInstance] {
	self: SubmitToTurnitinState =>

	override def describe(d: Description) = d.assignment(assignment)
}

trait SubmitToTurnitinValidation extends SelfValidating {
	self: SubmitToTurnitinRequest with FeaturesComponent =>

	override def validate(errors: Errors) {
		if (!features.turnitinSubmissions) errors.reject("turnitin.submissions.disabled")

		if (!Option(submitter).exists(_.isFoundUser))
			errors.rejectValue("submitter", "userId.notfound")
	}
}