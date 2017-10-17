package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, LenientGroupService}
import uk.ac.warwick.userlookup.User


object AcademicOfficeMembershipNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/manual_membership_eo.ftl"
}

@Entity
@DiscriminatorValue("ExamsOfficeManualMembershipSummary")
class AcademicOfficeMembershipNotification extends Notification[Department, Unit]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	lazy val RecipientUsercode: String = Wire.optionProperty("${academicoffice.notificationrecipient}").getOrElse(
		throw new IllegalStateException("academicoffice.notificationrecipient property is missing")
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
		val user = userLookup.getUserByUserId(RecipientUsercode)
		if (!user.isFoundUser) throw new IllegalStateException(s"No recipient found with the usercode - $RecipientUsercode")
		Seq(user)
	}
}
