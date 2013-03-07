package uk.ac.warwick.tabula.data.model

import scala.collection._
import scala.collection.JavaConversions._
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class UserSettings extends GeneratedId with SettingsMap[UserSettings] with PermissionsTarget {
	import UserSettings._
	
	var userId: String = _
	
	def getAlertsSubmission = alertsSubmission
	def alertsSubmission = getStringSetting(Settings.AlertsSubmission).orNull
	def alertsSubmission_= (alert: String) = settings += (Settings.AlertsSubmission -> alert)
	
	def getHiddenIntros = hiddenIntros
	def hiddenIntros = getStringSeqSetting(Settings.HiddenIntros).orNull
	def hiddenIntros_= (hiddenIntro: String) = settings += (Settings.HiddenIntros -> hiddenIntro)
		
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
		val HiddenIntros = "hiddenIntros"
	}
}
