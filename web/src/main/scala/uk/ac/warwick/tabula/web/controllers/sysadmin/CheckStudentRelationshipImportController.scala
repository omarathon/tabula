package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.sysadmin._
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent
import uk.ac.warwick.tabula.web.Mav



@Controller
@RequestMapping(value = Array("/sysadmin/relationships/check"))
class CheckStudentRelationshipImportController extends BaseSysadminController with AutowiringRelationshipServiceComponent {

	@ModelAttribute("checkStudentRelationshipImportCommand") def checkStudentRelationshipImportCommand = CheckStudentRelationshipImportCommand()


	@RequestMapping(method=Array(GET, HEAD), params = Array("!student"))
	def form(): Mav = Mav("sysadmin/relationships/check").crumbs(SysadminBreadcrumbs.Relationships.Home)

	@RequestMapping(method = Array(GET, HEAD), params = Array("student"))
	def check(@ModelAttribute("checkStudentRelationshipImportCommand") cmd: Appliable[CheckStudentRelationshipImport]): Mav =
		Mav("sysadmin/relationships/check", "result" -> cmd.apply()).crumbs(SysadminBreadcrumbs.Relationships.Home)

}