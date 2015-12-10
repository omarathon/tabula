package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

import scala.collection._

@Entity
class UserSettings extends GeneratedId with SettingsMap with HasNotificationSettings with PermissionsTarget {
	import UserSettings._

	@transient
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@Column(unique = true)
	var userId: String = _

	def alertsSubmission = getStringSetting(Settings.AlertsSubmission) getOrElse(AlertsNoteworthySubmissions)
	def alertsSubmission_= (alert: String) = settings += (Settings.AlertsSubmission -> alert)

	def hiddenIntros = getStringSeqSetting(Settings.HiddenIntros) getOrElse(Nil)
	def hiddenIntros_= (hiddenIntro: Seq[String]) = settings += (Settings.HiddenIntros -> hiddenIntro)

	// It's okay for this to be null - the null choice is to use the department's setting
	def weekNumberingSystem = getStringSetting(Settings.WeekNumberingSystem).orNull
	def weekNumberingSystem_= (wnSystem: String) = settings += (Settings.WeekNumberingSystem -> wnSystem)

	def bulkEmailSeparator = getStringSetting(Settings.BulkEmailSeparator) getOrElse(DefaultBulkEmailSeparator)
	def bulkEmailSeparator_= (separator: String) = settings += (Settings.BulkEmailSeparator -> separator)

	def profilesDefaultView = getStringSetting(Settings.ProfilesDefaultView).getOrElse(DefaultProfilesDefaultView)
	def profilesDefaultView_= (view: String) = settings += (Settings.ProfilesDefaultView -> view)

	// Active department should be Optional; if the user has chosen one yet they must pick the initial value
	def activeDepartment: Option[Department] = getStringSetting(Settings.ActiveDepartment).flatMap(moduleAndDepartmentService.getDepartmentByCode)
	def activeDepartment_= (department: Department) = settings += (Settings.ActiveDepartment -> department.code)

	// Active academic year should be Optional; we handle getting the latest year eslewhere
	def activeAcademicYear: Option[AcademicYear] = getIntSetting(Settings.ActiveAcademicYear).map(y => AcademicYear(y))
	def activeAcademicYear_= (academicYear: AcademicYear) = settings += (Settings.ActiveAcademicYear -> academicYear.startYear)

	def string(key: String) = getStringSetting(key).orNull

	def this(userId: String) = {
		this()
		this.userId = userId
	}

	override def toString = "UserSettings [" + settings + "]"

	def permissionsParents = Stream.empty
}

object UserSettings {
	val AlertsAllSubmissions = "allSubmissions"
	val AlertsNoteworthySubmissions = "lateSubmissions"
	val AlertsNoSubmissions = "none"

	val DefaultBulkEmailSeparator = ";"

	val DefaultProfilesDefaultView = "gadget"

	object Settings {
		val AlertsSubmission = "alertsSubmission"
		val HiddenIntros = "hiddenIntros"
		val WeekNumberingSystem = "weekNumberSystem"
		val BulkEmailSeparator = "bulkEmailSeparator"
		val ProfilesDefaultView = "profilesDefaultView"
		val ActiveDepartment = "activeDepartment"
		val ActiveAcademicYear = "activeAcademicYear"
			
		def hiddenIntroHash(mappedPage: String, setting: String) = {
			val popover = mappedPage + ":" + setting
			val shaHash = DigestUtils.shaHex(popover)

			shaHash
		}
	}
}
