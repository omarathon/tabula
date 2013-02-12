package uk.ac.warwick.tabula.data.model

import scala.collection._
import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class UserSettings extends GeneratedId with SettingsMap[UserSettings] with PermissionsTarget {
	
	@BeanProperty var userId: String = _
		
	def this(userId: String) = {
		this()
		this.userId = userId		
	}
			
	override def toString = "UserSettings [" + settings + "]"
	
	def permissionsParents = Seq()
	
}

object UserSettings {
	val AlertsAllSubmissions = "allSubmissions"
	val AlertsLateSubmissions = "lateSubmissions"
	val AlertsNoSubmissions = "none"
	
	object Settings {
		val AlertsSubmission = "alertsSubmission"
	}
}
