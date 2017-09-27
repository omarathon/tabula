package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, LenientGroupService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.util.Try


object AcademicOfficeMembershipNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/manual_membership_eo.ftl"
}

@Entity
@DiscriminatorValue("ExamsOfficeManualMembershipSummary")
class AcademicOfficeMembershipNotification extends Notification[Department, Unit]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	lazy val RecipientWebGroup: String = Wire.optionProperty("${permissions.academicoffice.group}").getOrElse(
		throw new IllegalStateException("permissions.academicoffice.group property is missing")
	)
	@transient
	def groupService: LenientGroupService = userLookup.getGroupService

	priority = Warning

	def departments: Seq[Department] = entities


	def verb = "view"
	def title: String = s"${departments.size} have assignments or small group sets with manually added students."
	def url: String = Routes.adminHome
	def urlTitle = s"Sign in to Tabula"

	def content = FreemarkerModel(AcademicOfficeMembershipNotification.templateLocation, Map (
		"departments" -> departments
	))

	@transient
	override def recipients: Seq[User] = {
		val users = userLookup.getUsersByUserIds(groupService.getUserCodesInGroup(RecipientWebGroup)).values.asScala.toSeq
		if (users.isEmpty) throw new IllegalStateException(s"No users found in the recipient webgroup - $RecipientWebGroup")
		users
	}
}
