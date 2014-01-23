package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.AuditEventIndexService
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.commands._
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.permissions.Permissions
import AuditLogQueryCommand._
import uk.ac.warwick.tabula.services.AuditEventIndexServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAuditEventServiceComponent
import uk.ac.warwick.tabula.services.AuditEventServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAuditEventIndexServiceComponent
import uk.ac.warwick.tabula.helpers.StringUtils._

object AuditLogQueryCommand {
			
	type AuditLogQueryCommand = Appliable[AuditLogQueryResults] with AuditLogQueryCommandState
	
	val pageSize = 100
	
	def apply(): AuditLogQueryCommand =
		new AuditLogQueryCommandInternal
			with ComposableCommand[AuditLogQueryResults]
			with AuditLogQueryCommandPermissions
			with AutowiringAuditEventServiceComponent
			with AutowiringAuditEventIndexServiceComponent
			with Unaudited with ReadOnly
			
	case class AuditLogQueryResults(
		results: Seq[AuditEvent],
		pageNumber: Int,
		startIndex: Int,
		endIndex: Int
	)
}

class AuditLogQueryCommandInternal extends CommandInternal[AuditLogQueryResults] with AuditLogQueryCommandState with TaskBenchmarking {
	self: AuditEventServiceComponent with AuditEventIndexServiceComponent =>
		
	def applyInternal() = {
		val start = (page * pageSize) + 1
		val max = pageSize
		val end = start + max - 1
		val recent = benchmarkTask("Query audit event index") {
			if (query.hasText) {
				auditEventIndexService.openQuery(query, page * pageSize, pageSize)
			} else {
				auditEventIndexService.listRecent(page * pageSize, pageSize)
			}
		}
		
		AuditLogQueryResults(
			results = benchmarkTask("Parse into rich audit items") { recent.map(toRichAuditItem) },
			pageNumber = page,
			startIndex = start,
			endIndex = end
		)
	}
	
	def toRichAuditItem(item: AuditEvent) = item.copy(parsedData = auditEventService.parseData(item.data))
}

trait AuditLogQueryCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ViewAuditLog)
	}
}

trait AuditLogQueryCommandState {
	var page: Int = 0
	var query: String = ""
}

@Controller
@RequestMapping(Array("/sysadmin/audit/search"))
class AuditLogController extends BaseSysadminController {
	
	var auditEventIndexService = Wire[AuditEventIndexService]
	
	@ModelAttribute("auditLogQuery") def command: AuditLogQueryCommand = AuditLogQueryCommand()
	@ModelAttribute("lastIndexTime") def lastIndexTime = auditEventIndexService.lastIndexTime
	@ModelAttribute("lastIndexDuration") def lastIndexDuration = auditEventIndexService.lastIndexDuration

	@RequestMapping
	def searchAll(@ModelAttribute("auditLogQuery") command: AuditLogQueryCommand): Mav = {
		val results = command.apply()
		
		Mav("sysadmin/audit/list",
			"items" -> results.results,
			"fromIndex" -> true,
			"page" -> results.pageNumber,
			"startIndex" -> results.startIndex,
			"endIndex" -> results.endIndex)
			.crumbs(Breadcrumbs.Current("Sysadmin audit log"))
	}

}