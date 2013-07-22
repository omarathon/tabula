package uk.ac.warwick.tabula.data.model

import scala.collection._
import scala.collection.JavaConversions._
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.apache.commons.codec.digest.DigestUtils

@Entity
class UserSettings extends GeneratedId with SettingsMap[UserSettings] with PermissionsTarget {
	import UserSettings._

	@Column(unique = true)
	var userId: String = _
	
	def alertsSubmission = getStringSetting(Settings.AlertsSubmission).orNull
	def alertsSubmission_= (alert: String) = settings += (Settings.AlertsSubmission -> alert)
	
	def hiddenIntros = getStringSeqSetting(Settings.HiddenIntros) getOrElse(Nil)
	def hiddenIntros_= (hiddenIntro: Seq[String]) = settings += (Settings.HiddenIntros -> hiddenIntro)
	
	// It's okay for this to be null - the null choice is to use the department's setting
	def weekNumberingSystem = getStringSetting(Settings.WeekNumberingSystem).orNull
	def weekNumberingSystem_= (wnSystem: String) = settings += (Settings.WeekNumberingSystem -> wnSystem)
		
	def this(userId: String) = {
		this()
		this.userId = userId		
	}
			
	override def toString = "UserSettings [" + settings + "]"
	
	def permissionsParents = Stream.empty
}

object UserSettings {
	val AlertsAllSubmissions = "allSubmissions"
	val AlertsLateSubmissions = "lateSubmissions"
	val AlertsNoSubmissions = "none"
	
	object Settings {
		val AlertsSubmission = "alertsSubmission"
		val HiddenIntros = "hiddenIntros"
		val WeekNumberingSystem = "weekNumberSystem"
			
		def hiddenIntroHash(mappedPage: String, setting: String) = {
			val popover = mappedPage + ":" + setting
			val shaHash = DigestUtils.shaHex(popover)
			
			shaHash
		}
	}
}
