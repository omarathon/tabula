package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.sysadmin.{UserAccessManagerAuditCommand, UserAccessManagerWithDepartments}
import uk.ac.warwick.tabula.data.model.notifications.{UAMAuditFirstNotification, UAMAuditNotification, UAMAuditSecondNotification}
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
class UserAccessManagerAuditController extends BaseSysadminController with PermissionsServiceComponent {

	override def permissionsService: PermissionsService = Wire[PermissionsService]

	@ModelAttribute("userAccessManagerAuditCommand")
	def command(
		@RequestParam(value = "notificationChoice", required = false) notificationChoice: String,
		user: CurrentUser
	): Option[Appliable[Seq[UserAccessManagerWithDepartments]]] = {

		(notificationChoice match {
			case "first" => Some(new UAMAuditFirstNotification)
			case "second" => Some(new UAMAuditSecondNotification)
			case _ => None
		}).map(UserAccessManagerAuditCommand.apply)
	}

	@RequestMapping(method = Array(POST), value = Array("/sysadmin/uam-audit"))
	def adminModule(
		@ModelAttribute("userAccessManagerAuditCommand") command: Option[Appliable[Seq[UserAccessManagerWithDepartments]]],
	): Mav = command.map(_.apply()).map(_ => success).getOrElse(error)

	@RequestMapping(method = Array(GET), value = Array("/sysadmin/uam-audit"))
	def home: Mav = Mav("sysadmin/uam-audit").addObjects("uamUsercodes" -> uams.map(_.getUserId))

	def success: Mav = home.addObjects("success" -> true)

	def error: Mav = home.addObjects("error" -> true)

	def uams: Seq[User] = {
		permissionsService
			.getAllGrantedRolesForDefinition(UserAccessMgrRoleDefinition)
			.flatMap(_.users.users)
			.distinct
	}
}
