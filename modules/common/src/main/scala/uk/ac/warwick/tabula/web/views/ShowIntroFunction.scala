package uk.ac.warwick.tabula.web.views
import org.springframework.beans.factory.annotation.Autowired

import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService

/**
 * Freemarker directive to confirm whether an introductory popover should show automatically
 */
class ShowIntroFunction extends TemplateMethodModelEx {

	@Autowired var userSettings: UserSettingsService = _

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]
		
		if (arguments == null || args.size() != 1) throw new TemplateModelException("Invalid number of arguments")
		
		val currentUser = RequestInfo.fromThread.get.user
		val mappedPage = RequestInfo.mappedPage
		
		val setting = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		
		val shaHash = UserSettings.Settings.hiddenIntroHash(mappedPage, setting)
			
		(userSettings.getByUserId(currentUser.apparentId) match {
			case Some(settings) if settings.hiddenIntros != null => !(settings.hiddenIntros contains(shaHash))
			case _ => true
		}): java.lang.Boolean
	}
}