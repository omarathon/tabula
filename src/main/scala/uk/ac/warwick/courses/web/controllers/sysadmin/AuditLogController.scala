package uk.ac.warwick.courses.web.controllers.sysadmin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.services.AuditEventService
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.services.AuditEventIndexService

@Controller
class AuditLogController extends BaseController {

	@Autowired var auditEventService:AuditEventService =_
	@Autowired var auditEventIndexService:AuditEventIndexService =_
	
	val pageSize = 100
	
	@RequestMapping(value=Array("/sysadmin/audit/list"))
	def listAll(@RequestParam(value="page", defaultValue="0") page:Int) :Mav = {
		val start = (page*pageSize) + 1
		val max = pageSize
		val end = start+max - 1
		val recent = auditEventService.listRecent(page*pageSize, pageSize)
		Mav("sysadmin/audit/list", 
				"items" -> recent, 
				"fromIndex" -> false,
				"page" -> page,
				"startIndex" -> start,
				"endIndex" -> end)
	}
	
	@RequestMapping(value=Array("/sysadmin/audit/search"))
	def searchAll(@RequestParam(value="page", defaultValue="0") page:Int) :Mav = {
		val start = (page*pageSize) + 1
		val max = pageSize
		val end = start+max - 1
		val recent = auditEventIndexService.listRecent(page*pageSize, pageSize)
		
		Mav("sysadmin/audit/list", 
				"items" -> recent, 
				"fromIndex" -> true,
				"lastIndexTime" -> auditEventIndexService.lastIndexTime,
				"lastIndexDuration" -> auditEventIndexService.lastIndexDuration,
				"page" -> page,
				"startIndex" -> start,
				"endIndex" -> end)
	}
	
}