package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import javax.servlet.http.HttpServletResponse

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

/**
	* Creates jobs that are only available on the scheduler e.g. ImportMembersJob.
	* On a sysadmin path so only sysadmins can create jobs in this way.
	* Use a TrustedApplications link to proxy jobs on behalf of end-users.
	*/
@Controller
@RequestMapping(Array("/sysadmin/jobs/create"))
class CreateSchedulerJobController extends BaseController with AutowiringJobServiceComponent {

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def search(@RequestBody request: CreateSchedulerJobRequest)(implicit response: HttpServletResponse) = {
		val jobPrototype = request.createPrototype
		val user = request.onBehalfOf

		if (!user.isFoundUser) {
			Mav(new JSONView(Map(
				"success" -> false,
				"status" -> "bad_request",
				"result" -> Map("onBehalfOf" -> "User not found"),
				"errors" -> Seq(Map("field" -> "onBehalfOf", "message" -> "User not found"))
			)))
		} else {
			try {
				val instance = jobService.add(user, jobPrototype)
				Mav(new JSONView(Map(
					"success" -> true,
					"result" -> Map(
						"jobId" -> instance.id
					))
				))
			} catch {
				case e: Exception =>
					Mav(new JSONView(Map(
						"success" -> false,
						"status" -> "bad_request",
						"errors" -> Seq(e.getMessage)
					)))
			}
		}
	}

}

@JsonAutoDetect
class CreateSchedulerJobRequest extends Serializable {

	@BeanProperty var identifier: String = _
	@BeanProperty var data: JMap[String, Object] = _
	@BeanProperty var onBehalfOf: User = _

	def createPrototype: JobPrototype = JobPrototype(identifier, data.asScala.toMap)

}