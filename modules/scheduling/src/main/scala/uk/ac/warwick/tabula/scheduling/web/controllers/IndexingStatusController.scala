package uk.ac.warwick.tabula.scheduling.web.controllers

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventIndexService
import uk.ac.warwick.tabula.web.controllers.BaseController

import scala.concurrent.Await
import scala.concurrent.duration._

@Controller
@RequestMapping(Array("/index/nagios"))
class IndexingStatusController extends BaseController {
	import IndexingStatusController._

	var auditEventService = Wire[AuditEventService]
	var auditEventIndexService = Wire[AuditEventIndexService]

	@RequestMapping(Array("/status"))
	def lastrun(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		val latestDb = auditEventService.latest
		val latestIndex = Await.result(auditEventIndexService.newestItemInIndexDate, 10.seconds).getOrElse(new DateTime(0L))
		val latestIndexMinutesAgo = (latestDb.getMillis - latestIndex.getMillis) / MillisInAMinute

		val allDetails = s"latestDatabase,${latestDb.getMillis},latestIndex,${latestIndex.getMillis},latestIndexMinutesAgo,$latestIndexMinutesAgo"

		response.addHeader("Content-Type", "text/plain")
		response.addHeader("Content-Length", allDetails.length.toString)
		response.getWriter.write(allDetails)
	}

}

object IndexingStatusController {
	val MillisInASecond = 1000
	val MillisInAMinute = MillisInASecond * 60
}