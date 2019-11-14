package uk.ac.warwick.tabula.web.views

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCache}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget, ScopelessPermission, SelectorPermission}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}

/**
  * Freemarker directive to show the contents of the tag
  */
class PermissionFunction extends BaseTemplateMethodModelEx with Logging {
  @Autowired var securityService: SecurityService = _

  override def execMethod(arguments: Seq[_]): Object = {
    val currentUser = RequestInfo.fromThread.get.user

    arguments match {
      case Seq(actionName: String, item: PermissionsTarget) => RequestLevelCache.cachedBy("PermissionFunction.exec", s"$actionName-${item.id}") {
        securityService.can(currentUser, Permissions.of(actionName), item): JBoolean
      }

      case Seq(as: String, actionName: String, item: PermissionsTarget) if as == "real" => RequestLevelCache.cachedBy("PermissionFunction.exec", s"$as-$actionName-${item.id}") {
        val realUser = new CurrentUser(currentUser.realUser, currentUser.realUser)
        securityService.can(realUser, Permissions.of(actionName), item): JBoolean
      }

      // FIXME hard-coded
      case Seq(actionName: String, item: PermissionsTarget, selector: StudentRelationshipType) => RequestLevelCache.cachedBy("PermissionFunction.exec", s"$actionName-${item.id}-$selector") {
        securityService.can(currentUser, SelectorPermission.of(actionName, selector), item): JBoolean
      }

      case Seq(actionName: String) => RequestLevelCache.cachedBy("PermissionFunction.exec", actionName) {
        Permissions.of(actionName) match {
          case scopelessPermission: ScopelessPermission => securityService.can(currentUser, scopelessPermission): JBoolean
          case _ => throw new IllegalArgumentException("Not a scopeless permission")
        }
      }

      case _ => throw new IllegalArgumentException("Bad args")
    }
  }
}
