package uk.ac.warwick.courses.web.controllers.admin

import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.commands.feedback._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.services.AuditEventIndexService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadFeedback extends BaseController {
	@Autowired var feedbackDao: FeedbackDao = _
	@Autowired var fileServer: FileServer = _

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable feedbackId: String, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))

		feedbackDao.getFeedback(feedbackId) match {
			case Some(feedback) => {
				mustBeLinked(feedback, assignment)
				val renderable = new AdminGetSingleFeedbackCommand(feedback).apply()
				fileServer.serve(renderable, response)
			}
			case None => throw new ItemNotFoundException
		}
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download-zip/{filename}"))
class DownloadAllFeedback extends BaseController {
	@Autowired var fileServer: FileServer = _
	@RequestMapping
	def download(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val renderable = new AdminGetAllFeedbackCommand(assignment).apply()
		fileServer.serve(renderable, response)
	}
}

@Configurable @Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/list"))
class ListFeedback extends BaseController {

	@Autowired var auditIndexService: AuditEventIndexService = _

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module: Module, @PathVariable assignment: Assignment) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/list",
			"whoDownloaded" -> auditIndexService.whoDownloadedFeedback(assignment))
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

