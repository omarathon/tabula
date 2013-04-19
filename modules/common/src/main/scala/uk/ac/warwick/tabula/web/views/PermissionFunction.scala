package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.JavaImports._
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
import uk.ac.warwick.tabula.JavaImports._

/**
 * Freemarker directive to show the contents of the tag
 */
class PermissionFunction extends TemplateMethodModelEx with Logging {

	@Autowired var securityService: SecurityService = _

	override def exec(args: JList[_]): Object = {
		val arguments = args.asInstanceOf[JList[TemplateModel]]

		val request = RequestInfo.fromThread.get
		val currentUser = request.user

		val actionName = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		val item = DeepUnwrap.unwrap(arguments.get(1)).asInstanceOf[PermissionsTarget]
		val permission = Permissions.of(actionName)

		securityService.can(currentUser, permission, item): JBoolean

	}

}