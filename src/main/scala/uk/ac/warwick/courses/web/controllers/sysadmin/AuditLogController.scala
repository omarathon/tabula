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
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.web.Routes
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.courses.data.model.AuditEvent
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.userlookup.UserLookupInterface

class AuditLogQuery {
	@BeanProperty var page:Int = 0
	@BeanProperty var query:String = "" 
}

@Configurable
case class UserLookupQuery() {
	@Autowired var userLookup:UserLookupInterface =_
	@BeanProperty var userId:String =_
	@BeanProperty var uniId:String =_
	
	def user = {
		
	}
}

@Controller
class AuditLogController extends BaseController {

	@Autowired var auditEventService:AuditEventService =_
	@Autowired var auditEventIndexService:AuditEventIndexService =_
	@Autowired var json:ObjectMapper =_
	
	val pageSize = 100
	
	@RequestMapping(value=Array("/sysadmin/audit/list"))
	def listAll(query:AuditLogQuery) :Mav = {
		val page = query.page
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
	
	@RequestMapping(value=Array("/sysadmin/userlookup"))
	def whois(query:UserLookupQuery) = Mav("sysadmin/userlookup").noLayout()
	
	@RequestMapping(value=Array("/sysadmin/audit/search"))
	def searchAll(query:AuditLogQuery): Mav = {
		val page = query.page
		val start = (page*pageSize) + 1
		val max = pageSize
		val end = start+max - 1
		val recent = if (query.query.hasText) {
			auditEventIndexService.openQuery(query.query, page*pageSize, pageSize)
		} else {
			auditEventIndexService.listRecent(page*pageSize, pageSize)
		}
		Mav("sysadmin/audit/list", 
				"items" -> (recent map toRichAuditItem), 
				"Routes" -> Routes,
				"fromIndex" -> true,
				"lastIndexTime" -> auditEventIndexService.lastIndexTime,
				"lastIndexDuration" -> auditEventIndexService.lastIndexDuration,
				"page" -> page,
				"startIndex" -> start,
				"endIndex" -> end)
	}
	
	def toRichAuditItem(item:AuditEvent) = item.copy(parsedData=auditEventService.parseData(item.data))
	
}