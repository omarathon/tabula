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

object SubmitToTurnitinCommand {
	type CommandType = Appliable[JobInstance] with SubmitToTurnitinRequest with SelfValidating

	/**
		* Creates a job that submits the assignment to Turnitin.
		*
		* Returns the job instance ID for status tracking.
		*/
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
	def user: CurrentUser
}

trait SubmitToTurnitinRequest extends SubmitToTurnitinState {
	// No request params currently
}

abstract class SubmitToTurnitinCommandInternal(val module: Module, val assignment: Assignment, val user: CurrentUser)
	extends CommandInternal[JobInstance]
		with SubmitToTurnitinRequest {
	self: JobServiceComponent with FeaturesComponent =>

	override def applyInternal() = {
		if (features.turnitinLTI) jobService.add(Option(user), SubmitToTurnitinLtiJob(assignment))
		else jobService.add(Option(user), SubmitToTurnitinJob(assignment))
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
	self: SubmitToTurnitinState with FeaturesComponent =>

	override def validate(errors: Errors) {
		if (!features.turnitinSubmissions) errors.reject("turnitin.submissions.disabled")
	}
}