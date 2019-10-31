package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateModelException
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.permissions.PermissionsService

/**
  * Returns a list of users with the current role, ordered by surname, firstname.
  *
  * Accepts exactly two arguments, the role definition and the scope
  */
class UsersWithRoleFunction extends BaseTemplateMethodModelEx {

  @Autowired var permissionsService: PermissionsService = _
  @Autowired var userLookup: UserLookupService = _
  lazy val roleDefinitionConverter = new BuiltInRoleDefinitionUserType

  override def execMethod(arguments: Seq[_]): Object = {
    if (arguments == null || arguments.length != 2) throw new TemplateModelException("Invalid number of arguments")

    val roleName = arguments.head.asInstanceOf[String]

    val roleDefinition =
      permissionsService.getCustomRoleDefinitionById(roleName).getOrElse {
        roleDefinitionConverter.convertToObject(roleName)
      }

    val scope = arguments(1).asInstanceOf[PermissionsTarget]

    for {
      role <- permissionsService.getGrantedRole(scope, roleDefinition).toSeq
      user <- role.users.users
    } yield user
  }

}
