package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.imports.ImportAssignmentsCommand
import uk.ac.warwick.tabula.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.services.AssignmentImporter
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.services.ProfileIndexService

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 * 
 * @deprecated Use version in home module instead
 */

abstract class BaseSysadminController extends BaseController {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var userLookup = Wire.auto[UserLookupInterface]
	var urlRewriter = Wire.auto[UrlMethodModel]

	def redirectToHome = {
		// Redirect cross-context
		Redirect(urlRewriter.exec(ArrayList("/sysadmin/", "/", true)).toString())
	}
}

@Controller 
class HomeController extends BaseSysadminController {
	@RequestMapping(Array("/")) def home = redirectToHome
}

class ReindexAuditEventsForm {
	var indexer = Wire.auto[AuditEventIndexService]

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var from: DateTime = _

	def reindex = {
		indexer.indexFrom(from)
	}
}

class ReindexProfilesForm {
	var indexer = Wire.auto[ProfileIndexService]

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var from: DateTime = _

	def reindex = {
		indexer.indexFrom(from)
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-audit"))
class SysadminIndexAuditController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexAuditEventsForm
	
	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexAuditEventsForm) = {
		form.reindex
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-profiles"))
class SysadminIndexProfilesController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexProfilesForm
	
	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexProfilesForm) = {
		form.reindex
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import"))
class SysadminController extends BaseSysadminController {

	@RequestMapping(method = Array(POST))
	def importModules = {
		new ImportModulesCommand().apply()
		"sysadmin/importdone"
	}

}

@Controller
@RequestMapping(Array("/sysadmin/import-sits"))
class ImportSitsController extends BaseSysadminController {
	var importer = Wire.auto[AssignmentImporter]

	@RequestMapping(method = Array(POST))
	def reindex() = {
		val command = new ImportAssignmentsCommand
		command.apply()
		redirectToHome
	}
}