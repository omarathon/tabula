package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.tabula.home.commands.departments.AddDeptOwnerCommand
import uk.ac.warwick.tabula.home.commands.departments.RemoveDeptOwnerCommand
import uk.ac.warwick.tabula.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.services.AssignmentImporter
import uk.ac.warwick.tabula.commands.imports.ImportAssignmentsCommand
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.tabula.helpers.ArrayList

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

class ReindexForm {
	var indexer = Wire.auto[AuditEventIndexService]

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var from: DateTime = _

	def reindex = {
		indexer.indexFrom(from)
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run"))
class SysadminIndexController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexForm
	
	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexForm) = {
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