package uk.ac.warwick.tabula.coursework.web.controllers.admin

import collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadSubmissionsCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.ReadableInstant
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionListItem
import uk.ac.warwick.tabula.data.model.Assignment

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/list"))
class ListSubmissionsController extends CourseworkController {

	@RequestMapping(method = Array(GET, HEAD))
	def list(command: ListSubmissionsCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))

		val submissions = command.apply()
		val hasOriginalityReport = submissions.exists( _.submission.hasOriginalityReport )

		Mav("admin/assignments/submissions/list",
			"assignment" -> assignment,
			"submissions" -> submissions,
			"hasOriginalityReport" -> hasOriginalityReport)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}

