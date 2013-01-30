package uk.ac.warwick.tabula.web.views
import org.springframework.beans.factory.annotation.Autowired
import freemarker.core.Environment
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget

/**
 * Freemarker directive to show the contents of the tag
 */
class PermissionFunction /*[A <: Action[_] : ClassManifest]*/ extends TemplateMethodModelEx with Logging {

	@Autowired var securityService: SecurityService = _

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]

		val request = RequestInfo.fromThread.get
		val currentUser = request.user

		val actionName = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		val item = DeepUnwrap.unwrap(arguments.get(1)).asInstanceOf[PermissionsTarget]
		val permission = Permissions.of(actionName)

		securityService.can(currentUser, permission, item): java.lang.Boolean

	}

}