package uk.ac.warwick.courses.web.controllers.sysadmin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

import uk.ac.warwick.courses.services.AuditEventService
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav

@Controller
class AuditLogController extends BaseController {

	@Autowired var auditEventService:AuditEventService =_
	
	val pageSize = 100
	
	@RequestMapping(value=Array("/sysadmin/audit/list"))
	def listAll(@RequestParam(value="page", defaultValue="0") page:Int) :Mav = {
		val start = (page*pageSize) + 1
		val max = pageSize
		val end = start+max - 1
		val recent = auditEventService.listRecent(page*pageSize, pageSize)
		Mav("sysadmin/audit/list", 
				"items" -> recent, 
				"page" -> page,
				"startIndex" -> start,
				"endIndex" -> end)
	}
	
}