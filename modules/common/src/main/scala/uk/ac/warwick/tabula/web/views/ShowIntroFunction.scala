package uk.ac.warwick.tabula.web.views
import org.springframework.beans.factory.annotation.Autowired
import freemarker.core.Environment
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.UserSettingsService
import org.apache.commons.codec.digest.DigestUtils
import freemarker.template.TemplateMethodModelEx
import javax.servlet.http.HttpServletRequest
import freemarker.ext.beans.BeanModel
import org.springframework.web.servlet.HandlerMapping
import freemarker.template.TemplateModelException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.context.request.RequestAttributes

/**
 * Freemarker directive to confirm whether an introductory popover should show automatically
 */
class ShowIntroFunction extends TemplateMethodModelEx {

	@Autowired var userSettings: UserSettingsService = _

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]
		
		if (arguments == null || args.size() != 1) throw new TemplateModelException("Invalid number of arguments")
		
		val currentUser = RequestInfo.fromThread.get.user
		
		// get the @RequestMapping (without path variables resolved), so that users don't get the same popup again
		// for a given kind of page with only variables changing
		val requestAttributes = RequestContextHolder.getRequestAttributes.asInstanceOf[ServletRequestAttributes]
		val mappedPage = requestAttributes.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST).toString
		
		val popover = mappedPage + ":" + DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		val shaHash = DigestUtils.shaHex(popover)
	
		(userSettings.getByUserId(currentUser.apparentId) match {
			case Some(settings) if settings.hiddenIntros != null => settings.hiddenIntros contains(shaHash)
			case _ => false
		}): java.lang.Boolean
	}
}