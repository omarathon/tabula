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


trait UserSettingsService {
	def getByUserId(userId: String) : Option[UserSettings]
	def save(user: CurrentUser, usersettings: UserSettings)
	def parseJson(dataIn: String): Option[Map[String, Any]]
	def getSettings(user: User) : Option[Map[String, Any]]
	def getSetting(user: User, settingName: String) : Any 
}

@Service(value = "userSettingsService")
class UserSettingsServiceImpl extends UserSettingsService with Daoisms with Logging {

	@Autowired var json: ObjectMapper = _
	
	def getByUserId(userId: String) : Option[UserSettings] = {
		session.newCriteria[UserSettings]
			.add(Restrictions.eq("userId", userId))
			.uniqueResult
	}
	
	def save(user: CurrentUser, newSettings: UserSettings) =  {
		val existingSettings = getByUserId(user.apparentId)
		var settingsToSave = existingSettings match {
			case Some(settings) => settings
			case None => new UserSettings(user.apparentId)
		}
		
		val data = new StringWriter()
		json.writeValue(data, newSettings.fieldsMap)
		settingsToSave.data = data.toString
		
		session.saveOrUpdate(settingsToSave)
	}

	// parse the json settings into a map
	def parseJson(data: String): Option[Map[String, Any]] = try {
			val emptyJson = json.+("")
			Option(json.readValue(Option(data).getOrElse(emptyJson), classOf[Map[String, Any]]))
	} catch {
		case e @ (_: JsonParseException | _: JsonMappingException) => None
	}

	// get all settings for a user into a map
	def getSettings(user: User) : Option[Map[String, Any]] = {
	  getByUserId(user.getUserId) match {
			case Some(settings) => parseJson(settings.data)
			case None => None
		}
	}
	
	// get one setting for a user
	def getSetting(user: User, settingName: String) : Any = {
		getSettings(user) match {
			case Some(settings) => settings.getOrElse(settingName, None)
			case None => None
		}
	}
	
	
}
