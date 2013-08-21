package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.AuditEventIndexService
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.spring.Wire

class AuditLogQuery {
	var page: Int = 0
	var query: String = ""
}

case class UserLookupQuery() {
	var userLookup = Wire.auto[UserLookupInterface]
	var userId: String = _
	var uniId: String = _

	def user = {

	}
}

@Controller
class AuditLogController extends BaseSysadminController {

	var auditEventService = Wire.auto[AuditEventService]
	var auditEventIndexService = Wire.auto[AuditEventIndexService]
	var json = Wire.auto[ObjectMapper]

	val pageSize = 100

	@RequestMapping(value = Array("/sysadmin/audit/list"))
	def listAll(query: AuditLogQuery): Mav = {
		val page = query.page
		val start = (page * pageSize) + 1
		val max = pageSize
		val end = start + max - 1
		val recent = auditEventService.listRecent(page * pageSize, pageSize)
		Mav("sysadmin/audit/list",
			"items" -> recent,
			"fromIndex" -> false,
			"page" -> page,
			"startIndex" -> start,
			"endIndex" -> end)
			.crumbs(Breadcrumbs.Current("Sysadmin audit log"))
	}

	@RequestMapping(value = Array("/sysadmin/userlookup"))
	def whois(query: UserLookupQuery) = Mav("sysadmin/userlookup").noLayout()

	@RequestMapping(value = Array("/sysadmin/audit/search"))
	def searchAll(query: AuditLogQuery): Mav = {
		val page = query.page
		val start = (page * pageSize) + 1
		val max = pageSize
		val end = start + max - 1
		val recent = if (query.query.hasText) {
			auditEventIndexService.openQuery(query.query, page * pageSize, pageSize)
		} else {
			auditEventIndexService.listRecent(page * pageSize, pageSize)
		}
		Mav("sysadmin/audit/list",
			"items" -> (recent map toRichAuditItem),
			"fromIndex" -> true,
			"lastIndexTime" -> auditEventIndexService.lastIndexTime,
			"lastIndexDuration" -> auditEventIndexService.lastIndexDuration,
			"page" -> page,
			"startIndex" -> start,
			"endIndex" -> end)
			.crumbs(Breadcrumbs.Current("Sysadmin audit log"))
	}

	def toRichAuditItem(item: AuditEvent) = item.copy(parsedData = auditEventService.parseData(item.data))

}