package uk.ac.warwick.tabula.coursework.web.controllers.sysadmin

import uk.ac.warwick.tabula.coursework.web.controllers._
import org.springframework.stereotype._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.jobs.TestingJob

@Controller
@RequestMapping(value = Array("/sysadmin/jobs"))
class JobController extends CourseworkController {

	@Autowired var jobService: JobService = _

	@RequestMapping(value = Array("/list"))
	def list = {
		val jobs = jobService.unfinishedInstances.sortBy(_.createdDate).reverse
		Mav("sysadmin/jobs/list", "jobs" -> jobs)
	}

	@RequestMapping(value = Array("/create-test"), method = Array(POST))
	def test = {
		val id = jobService.add(Some(user), TestingJob("sysadmin test", 500))
		testStatus(id)
		Redirect("/sysadmin/jobs/job-status?id=" + id)
	}

	@RequestMapping(value = Array("/job-status"))
	def testStatus(@RequestParam("id") id: String) = {
		val instance = jobService.getInstance(id)
		Mav("sysadmin/jobs/job-status",
			"jobId" -> id,
			"jobStatus" -> (instance map (_.status) getOrElse (""))).noLayoutIf(ajax)
	}

}