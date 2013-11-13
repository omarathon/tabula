package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRequestRespondedNotification, ExtensionRequestRejectedNotification}
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.data.model.forms.Extension


class EditAssignmentCommand(module: Module = null, val assignment: Assignment = null, user: CurrentUser)
	extends ModifyAssignmentCommand(module) with NotificationHandling {

	private var unapprovedExtensions: Seq[Extension] = Seq()

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Assignment.Update, assignment)

	this.copyFrom(assignment)

	def canUpdateMarkingWorkflow = {
		Option(assignment.markingWorkflow) match {
			// if students can choose the marker and submissions exist then the markingWorkflow cannot be updated
			case Some(scheme) if scheme.studentsChooseMarker => (assignment.submissions.size() == 0)
			case Some(scheme) => true
			case None => true
		}
	}

	override def validate(errors: Errors) {
		super.validate(errors)

		if (academicYear != assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}

	override def contextSpecificValidation(errors:Errors){
		val workflowChanged = assignment.markingWorkflow != markingWorkflow
		if (!canUpdateMarkingWorkflow && workflowChanged){
			errors.rejectValue("markingWorkflow", "markingWorkflow.cannotChange")
		}
	}

	override def applyInternal(): Assignment = transactional() {
		copyTo(assignment)

		if (!allowExtensions && assignment.countUnapprovedExtensions > 0) {
			// reject unapproved extensions (in normal circumstances, this should be unlikely)
			unapprovedExtensions = assignment.extensionService.getUnapprovedExtensions(assignment)
			val admin = user.apparentUser
			unapprovedExtensions.foreach { e =>
				e.rejected = true

				// let's notify manually for completeness
				val student = userLookup.getUserByWarwickUniId(e.universityId)
				val studentNotification = new ExtensionRequestRejectedNotification(e, student, admin) with FreemarkerTextRenderer
				val adminNotification = new ExtensionRequestRespondedNotification(e, student, admin) with FreemarkerTextRenderer
				notify[Option[Extension]](Seq(studentNotification, adminNotification))
			}
		}

		assignment
	}

	override def describe(d: Description) {
		d.assignment(assignment).properties(
			"name" -> name,
			"openDate" -> openDate,
			"closeDate" -> closeDate
		)
		if (!unapprovedExtensions.isEmpty) {
			d.assignment(assignment).property(
				"studentExtensionRequestsAutoRejected" -> unapprovedExtensions.map(_.universityId)
			)
		}
	}

}
