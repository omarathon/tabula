package uk.ac.warwick.courses.web.views
import org.springframework.beans.factory.annotation.Autowired
import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateModel
import uk.ac.warwick.courses.actions.Action
import uk.ac.warwick.courses.services.SecurityService
import uk.ac.warwick.courses.actions.ActionTarget
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.helpers.Logging

/**
 * Freemarker directive to show the contents of the tag  
 */
class PermissionTag[A <: Action[_] : ClassManifest] extends TemplateDirectiveModel with Logging {
	
	@Autowired var securityService:SecurityService =_
	
	override def execute(env:Environment,
			params:java.util.Map[_,_],
			loopVars:Array[TemplateModel],
			body:TemplateDirectiveBody) = {
		
		val item = params.get("object").asInstanceOf[ActionTarget]
		val currentUser = params.get("user").asInstanceOf[CurrentUser]
		val action = Action.of[A](item)
		
		if ( securityService.can(currentUser, action) ) {
			if (debugEnabled) logger.debug("Rendering content for "+currentUser+" to "+action)
			body.render(env.getOut)
		} else {
			if (debugEnabled) logger.debug("Not rendering content for "+currentUser+" to "+action)
		}
	}
}