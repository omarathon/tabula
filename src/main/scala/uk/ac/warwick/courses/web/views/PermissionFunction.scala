package uk.ac.warwick.courses.web.views
import org.springframework.beans.factory.annotation.Autowired
import freemarker.core.Environment
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import uk.ac.warwick.courses.actions.Action
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.SecurityService
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.RequestInfo
import freemarker.template.TemplateMethodModelEx

/**
 * Freemarker directive to show the contents of the tag  
 */
class PermissionFunction/*[A <: Action[_] : ClassManifest]*/ extends TemplateMethodModelEx with Logging {
	
	@Autowired var securityService:SecurityService =_
	
	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]
		
		val request = RequestInfo.fromThread.get
		val currentUser = request.user
		
		val actionName = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		val item = DeepUnwrap.unwrap(arguments.get(1))
		val action = Action.of(actionName, item)

		securityService.can(currentUser, action) : java.lang.Boolean
		
	}
	
}