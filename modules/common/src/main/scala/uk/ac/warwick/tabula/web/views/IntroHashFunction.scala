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
 * Freemarker directive to get the hash of a setting on the current page
 */
class IntroHashFunction extends TemplateMethodModelEx {

	@Autowired var userSettings: UserSettingsService = _

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]
		
		if (arguments == null || args.size() != 1) throw new TemplateModelException("Invalid number of arguments")
		
		val mappedPage = RequestInfo.mappedPage		
		val setting = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]
		
		UserSettings.Settings.hiddenIntroHash(mappedPage, setting)
	}
}