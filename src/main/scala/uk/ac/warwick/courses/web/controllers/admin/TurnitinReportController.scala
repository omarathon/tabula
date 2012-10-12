package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import java.io.Writer
import uk.ac.warwick.courses._
import uk.ac.warwick.courses.web.controllers.BaseController
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.turnitin._
import uk.ac.warwick.courses.actions.Manage
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.courses.web.Mav

class ViewPlagiarismReportCommand {
	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var fileId: String = _
}

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 */
@Controller
class TurnitinReportController extends BaseController {

	@Autowired var turnitinService: Turnitin = _

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/turnitin-report/{fileId}"))
	def goToReport(command: ViewPlagiarismReportCommand): Mav = {
		mustBeLinked(command.assignment, command.module)
		mustBeAbleTo(Manage(command.module))

		debug("Getting document viewer URL for FileAttachment %s", command.fileId)
		
		val session = turnitinService.login(user)
		session match {
			case Some(session) => {

				val classId = Turnitin.classIdFor(command.assignment)
				val assignmentId = Turnitin.assignmentIdFor(command.assignment)
				session.listSubmissions(classId, assignmentId) match {
					case GotSubmissions(list) => {
						val matchingObject = list.find { _.title == command.fileId }
						val objectId = matchingObject.map { _.objectId }
						objectId match {
							case Some(id) => {
								debug("Found objectID %s for FileAttachment %s", id, command.fileId)
								val link = session.getDocumentViewerLink(id).toString
								debug("Redirecting to %s for FileAttachment %s", link, command.fileId)
								Redirect(link)
							}
							case None => {
								Mav("admin/assignments/turnitin/report_error", "problem" -> "no-object")
							}
						}
					}
					case what => Mav("admin/assignments/turnitin/report_error", "problem" -> "api-error", "message" -> what.message)
				}
				
			}
			case None => Mav("admin/assignments/turnitin/report_error", "problem" -> "no-session")
		}

	}
	
	private def goToReport(session: Session) = {
		
	}

}