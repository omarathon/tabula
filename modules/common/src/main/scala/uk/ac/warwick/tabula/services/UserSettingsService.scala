package uk.ac.warwick.tabula.services

import java.io.StringWriter
import uk.ac.warwick.tabula.data.model.UserSettings
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.criterion.Restrictions
import org.codehaus.jackson.map.JsonMappingException
import org.codehaus.jackson.JsonParseException
import org.springframework.beans.factory.annotation.Autowired
import org.codehaus.jackson.map.ObjectMapper
import uk.ac.warwick.tabula.CurrentUser
import scala.util.parsing.json.JSONObject
import scala.util.parsing.json.JSONArray
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire


trait UserSettingsService {
	def getByUserId(userId: String) : Option[UserSettings]
	def save(user: CurrentUser, usersettings: UserSettings)
}

@Service(value = "userSettingsService")
class UserSettingsServiceImpl extends UserSettingsService with Daoisms with Logging {

	val json = Wire.auto[ObjectMapper]
	
	def getByUserId(userId: String) : Option[UserSettings] = {
		session.newCriteria[UserSettings]
			.add(Restrictions.eq("userId", userId))
			.uniqueResult
	}
	
	def save(user: CurrentUser, newSettings: UserSettings) =  {
		val existingSettings = getByUserId(user.apparentId)
		val settingsToSave = existingSettings match {
			case Some(settings) => settings
			case None => new UserSettings(user.apparentId)
		}
		session.saveOrUpdate(settingsToSave ++= newSettings)
	}
	
}
