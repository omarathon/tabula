package uk.ac.warwick.courses.web.controllers.admin

import collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadSubmissionsCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.ReadableInstant
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.commands.assignments.SubmissionListItem
import uk.ac.warwick.courses.data.model.Assignment

@Configurable @Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions/list"))
class ListSubmissionsController extends BaseController {

	@RequestMapping(method=Array(GET, HEAD))
	def list(command:ListSubmissionsCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
		
		val submissions = command.apply()
    val hasOriginalityReport = submissions.exists(_.submission.originalityReport != null)

		Mav("admin/assignments/submissions/list",
				"assignment" -> assignment,
				"submissions" -> submissions,
        "hasOriginalityReport" -> hasOriginalityReport)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}

