package uk.ac.warwick.tabula.coursework.web.controllers.admin

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
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.actions.Manage
import uk.ac.warwick.tabula.coursework.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.data.model._
import uk.ac.warwick.tabula.coursework.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.services._
import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.AcademicYear
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.ItemNotFoundException
import uk.ac.warwick.tabula.coursework.services.AuditEventIndexService
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadFeedback extends BaseController {
	var feedbackDao = Wire.auto[FeedbackDao]
	var fileServer = Wire.auto[FileServer]

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
	var fileServer = Wire.auto[FileServer]
	
	@RequestMapping
	def download(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val renderable = new AdminGetAllFeedbackCommand(assignment).apply()
		fileServer.serve(renderable, response)
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/list"))
class ListFeedback extends BaseController {
	var auditIndexService = Wire.auto[AuditEventIndexService]

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module: Module, @PathVariable assignment: Assignment) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/list",
			"whoDownloaded" -> auditIndexService.whoDownloadedFeedback(assignment))
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

