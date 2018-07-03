package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.sysadmin.{UserAccessManagerAuditCommand, UserAccessManagerWithDepartments}
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
class UserAccessManagerAuditController extends BaseSysadminController with PermissionsServiceComponent {

	override def permissionsService: PermissionsService = Wire[PermissionsService]

	@RequestMapping(method = Array(GET), value = Array("/sysadmin/uam-audit"))
	def home: Mav = Mav("sysadmin/uam-audit").addObjects("uamUsercodes" -> uams.map(_.getUserId))

	@ModelAttribute("userAccessManagerAuditCommand")
	def command(
		@RequestParam(value = "notificationChoice", required = false) notificationChoice: String,
	): Appliable[Seq[UserAccessManagerWithDepartments]] = UserAccessManagerAuditCommand.apply(notificationChoice)

	@RequestMapping(method = Array(POST), value = Array("/sysadmin/uam-audit"))
	def adminModule(
		@ModelAttribute("userAccessManagerAuditCommand") command: Appliable[Seq[UserAccessManagerWithDepartments]],
	): Mav = {
		command.apply()
		success
	}

	def success: Mav = home.addObjects("success" -> true)

	def error: Mav = home.addObjects("error" -> true)

	def uams: Seq[User] = {
		permissionsService
			.getAllGrantedRolesForDefinition(UserAccessMgrRoleDefinition)
			.flatMap(_.users.users)
			.distinct
	}
}
