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

class ViewPlagiarismReportCommand {
	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var fileId: String = _
}

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 * 
 * The Turnitin API tells us the URL at Turnitin, and we 
 */
@Controller
class TurnitinReportController extends BaseController {

	@Autowired var turnitinService: Turnitin = _
	
	@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/turnitin-report/{fileId}"))
	def goToReport(command: ViewPlagiarismReportCommand, out: Writer) = {
		mustBeLinked(command.assignment, command.module)
		mustBeAbleTo(Manage(command.module))
		
		val session = turnitinService.login(user)
		session.map{ session =>
			
			val classId = Turnitin.classIdFor(command.assignment)
			val assignmentId = Turnitin.assignmentIdFor(command.assignment)
			session.listSubmissions(classId, assignmentId) match {
				case GotSubmissions(list) => {
					val matchingObject = list.find{ _.title == command.fileId }
					val objectId = matchingObject.map{ _.objectId }
					objectId match {
						case Some(id) => out.write(session.getDocumentViewerLink(id).toString)
						case None => out.write("Submission wasn't found in Turnitin")
					}
				}
				case _ => out.write("Failure while looking up submission details")
			}
			
		}
		
		if (!session.isDefined) {
			out.write("couldn't get a session")
		}
	}
	
}