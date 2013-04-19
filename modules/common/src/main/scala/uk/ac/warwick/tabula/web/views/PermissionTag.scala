package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.spring.Wire
import freemarker.core.Environment
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._

/**
 * Freemarker directive to show the contents of the tag
 */
class PermissionTag extends TemplateDirectiveModel with Logging {

	var securityService = Wire[SecurityService]

	override def execute(env: Environment,
		_params: JMap[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody) = {
		val params = _params.asInstanceOf[JMap[String, TemplateModel]]

		val request = RequestInfo.fromThread.get
		val currentUser = request.user

		val item = DeepUnwrap.unwrap(params.get("object")).asInstanceOf[PermissionsTarget]
		val actionName = DeepUnwrap.unwrap(params.get("action")).asInstanceOf[String]
		val permission = Permissions.of(actionName)

		if (securityService.can(currentUser, permission, item)) {
			if (debugEnabled) logger.debug("Rendering content for " + currentUser + " to " + permission + " on " + item)
			body.render(env.getOut)
		} else {
			if (debugEnabled) logger.debug("Not rendering content for " + currentUser + " to " + permission + " on " + item)
		}
	}
}