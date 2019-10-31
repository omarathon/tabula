package uk.ac.warwick.tabula.web.views

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.services.UserSettingsService

/**
  * Freemarker directive to retrieve a user setting
  */
class UserSettingFunction extends BaseTemplateMethodModelEx {

  @Autowired var userSettings: UserSettingsService = _

  override def execMethod(arguments: Seq[_]): Object = {
    val setting = arguments.toList match {
      case (setting: String) :: Nil => setting
      case _ => throw new IllegalArgumentException("Bad args: " + arguments)
    }

    val currentUser = RequestInfo.fromThread.get.user

    userSettings.getByUserId(currentUser.apparentId) match {
      case Some(settings) => settings.string(setting)
      case _ => null
    }
  }
}
