package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute, PathVariable}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitin._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment, Module}
import uk.ac.warwick.tabula.data.model.notifications.coursework.TurnitinJobSuccessNotification
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.OriginalityReportService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.turnitinlti.TurnitinLtiViewReportCommand
import uk.ac.warwick.tabula.services.turnitin.GotSubmissions
import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation

class ViewPlagiarismReportCommand(val module: Module, val assignment: Assignment, val fileId: String, val user: CurrentUser)
	extends Command[Mav] with ReadOnly with Unaudited with CompletesNotifications[Mav] {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, assignment)
	
	var turnitinService = Wire[Turnitin]
	var originalityReportService = Wire[OriginalityReportService]

	def applyInternal() = {
		debug("Getting document viewer URL for FileAttachment %s", fileId)
		
		turnitinService.login(user) match {
			case Some(session) =>

				val classId = Turnitin.classIdFor(assignment, turnitinService.classPrefix)
				val className = Turnitin.classNameFor(assignment)
				val assignmentId = Turnitin.assignmentIdFor(assignment)
				val assignmentName = Turnitin.assignmentNameFor(assignment)
				session.listSubmissions(classId, className, assignmentId, assignmentName) match {
					case GotSubmissions(list) =>
						val matchingObject = list.find { _.title == fileId }
						val objectId = matchingObject.map { _.objectId }
						objectId match {
							case Some(id) =>
								debug("Found objectID %s for FileAttachment %s", id, fileId)
								val link = session.getDocumentViewerLink(id).toString
								debug("Redirecting to %s for FileAttachment %s", link, fileId)
								Mav("redirect:" + link)
							case None =>
								Mav("admin/assignments/turnitin/report_error", "problem" -> "no-object")
						}
					case what => Mav("admin/assignments/turnitin/report_error", "problem" -> "api-error", "message" -> what.message)
				}
			case None => Mav("admin/assignments/turnitin/report_error", "problem" -> "no-session")
		}
	}

	def notificationsToComplete(commandResult: Mav): CompletesNotificationsResult = {
		commandResult.viewName.startsWith("redirect:") match {
			case true =>
				originalityReportService.getOriginalityReportByFileId(fileId).map(report =>
					CompletesNotificationsResult(
						notificationService.findActionRequiredNotificationsByEntityAndType[TurnitinJobSuccessNotification](report),
						user.apparentUser
					)
				).getOrElse(EmptyCompletesNotificationsResult)
			case false =>
				EmptyCompletesNotificationsResult
		}
	}
}

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 */
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/turnitin-lti-report/{attachment}"))
class TurnitinLtiReportController extends CourseworkController {

	@ModelAttribute("turnitinLtiViewReportCommand")
	def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("attachment") attachment: FileAttachment,
		user: CurrentUser
	) = {
		TurnitinLtiViewReportCommand(user)
	}

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def goToReport(@Valid @ModelAttribute("turnitinLtiViewReportCommand") command: Appliable[Mav], errors: Errors): Mav = command.apply()

}