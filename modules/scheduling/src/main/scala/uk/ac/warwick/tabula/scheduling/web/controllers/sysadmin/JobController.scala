package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.controllers._
import org.springframework.stereotype._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.jobs.TestingJob

class JobQuery {
	var page: Int = 0
}

@Controller
@RequestMapping(value = Array("/sysadmin/jobs"))
class JobController extends BaseController {

	@Autowired var jobService: JobService = _
	
	val pageSize = 100

	@RequestMapping(value = Array("/list"))
	def list(query: JobQuery) = {
		val unfinished = jobService.unfinishedInstances
		
		val page = query.page
		val start = (page * pageSize) + 1
		val max = pageSize
		val end = start + max - 1
		
		val recent = jobService.listRecent(page * pageSize, pageSize)
		
		Mav("sysadmin/jobs/list",
			"unfinished" -> unfinished,
			"finished" -> recent,
			"fromIndex" -> false,
			"page" -> page,
			"startIndex" -> start,
			"endIndex" -> end)
	}

	@RequestMapping(value = Array("/create-test"), method = Array(POST))
	def test = {
		val id = jobService.add(Some(user), TestingJob("sysadmin test", TestingJob.DefaultDelay)).id
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
	
	@RequestMapping(value = Array("/kill"))
	def kill(@RequestParam("id") id: String) = {
		val instance = jobService.getInstance(id)
		jobService.kill(instance.get)
		Redirect("/sysadmin/jobs/list")
	}

}