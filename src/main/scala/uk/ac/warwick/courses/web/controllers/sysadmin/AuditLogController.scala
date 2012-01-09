package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.web.controllers.Controllerism
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.`object`.MappingSqlQuery
import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AuditEventService

@Controller
class AuditLogController extends Controllerism with Daoisms {

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