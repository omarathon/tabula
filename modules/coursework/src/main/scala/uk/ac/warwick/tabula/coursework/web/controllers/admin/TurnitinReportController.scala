package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import java.io.Writer
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.services.turnitin._
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._

class ViewPlagiarismReportCommand(val module: Module, val assignment: Assignment, val fileId: String, val user: CurrentUser)
	extends Command[Mav] with ReadOnly with Unaudited {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, assignment)
	
	var turnitinService = Wire.auto[Turnitin]
	
	def applyInternal() = {
		debug("Getting document viewer URL for FileAttachment %s", fileId)
		
		val session = turnitinService.login(user)
		session match {
			case Some(session) => {

				val classId = Turnitin.classIdFor(assignment, turnitinService.classPrefix)
				val assignmentId = Turnitin.assignmentIdFor(assignment)
				session.listSubmissions(classId, assignmentId) match {
					case GotSubmissions(list) => {
						val matchingObject = list.find { _.title == fileId }
						val objectId = matchingObject.map { _.objectId }
						objectId match {
							case Some(id) => {
								debug("Found objectID %s for FileAttachment %s", id, fileId)
								val link = session.getDocumentViewerLink(id).toString
								debug("Redirecting to %s for FileAttachment %s", link, fileId)
								Mav("redirect:" + link)
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
	
}

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 */
@Controller
class TurnitinReportController extends CourseworkController {
	
	@ModelAttribute def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("fileId") fileId: String
	) = new ViewPlagiarismReportCommand(module, assignment, fileId, user)

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/turnitin-report/{fileId}"))
	def goToReport(command: ViewPlagiarismReportCommand): Mav = command.apply()

}