package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, LenientGroupService}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._


object ExamsOfficeMembershipNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/manual_membership_eo.ftl"
}

@Entity
@DiscriminatorValue("ExamsOfficeManualMembershipSummary")
class ExamsOfficeMembershipNotification extends Notification[Department, Unit]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	lazy val RecipientWebGroup: String = Wire.optionProperty("${examsoffice.manualmembership.recipients}").getOrElse(
		throw new IllegalStateException("examsoffice.manualmembership.recipients property is missing")
	)
	@transient
	def groupService: LenientGroupService = userLookup.getGroupService

	priority = Warning

	def departments: Seq[Department] = entities

	def numAssignments: Map[String, String] = getStringMapSetting("summary" , default=Map())
	def numAssignments_= (numAssignments:Map[String, String]) { settings += ("numAssignments" -> numAssignments) }

	def numSmallGroupSets: Map[String, String] = getStringMapSetting("numSmallGroupSets", default=Map())
	def numSmallGroupSets_= (numSmallGroupSets:Map[String, String]) { settings += ("numSmallGroupSets" -> numSmallGroupSets) }


	def verb = "view"
	def title: String = s"${departments.size} have assignments or small group sets with manually added students."
	def url: String = Routes.home
	def urlTitle = s"Sign in to Tabula"

	def content = FreemarkerModel(ManualMembershipWarningNotification.templateLocation, Map (
		"departments" -> departments,
		"numAssignments" -> numAssignments,
		"numSmallGroupSets" -> numSmallGroupSets
	))

	@transient
	override def recipients: Seq[User] = {
		val users = userLookup.getUsersByUserIds(groupService.getUserCodesInGroup(RecipientWebGroup)).values.asScala.toSeq
		if (users.isEmpty) throw new IllegalStateException(s"No users found in the recipient webgroup - $RecipientWebGroup")
		users
	}
}
