package uk.ac.warwick.tabula.web.views

import org.springframework.beans.factory.annotation.Autowired
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.{Logging, RequestLevelCache}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget, ScopelessPermission, SelectorPermission}
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

/**
  * Freemarker directive to show the contents of the tag
  */
class PermissionFunction extends TemplateMethodModelEx with Logging {
  @Autowired var securityService: SecurityService = _

  override def exec(args: JList[_]): Object = {
    val currentUser = RequestInfo.fromThread.get.user

    val arguments = args.asScala.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

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