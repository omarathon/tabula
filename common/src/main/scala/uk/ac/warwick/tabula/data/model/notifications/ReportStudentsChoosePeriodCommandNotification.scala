package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("ReportStudentsChoosePeriodCommandNotification")
class ReportStudentsChoosePeriodCommandNotification extends Notification[Department, Unit]
	with SingleRecipientNotification
	with SingleItemNotification[Department]
	with MyWarwickNotification {

	@transient
	val userLookup: UserLookupService = Wire[UserLookupService]

	@transient
	lazy val RecipientUsercode: String = Wire.optionProperty("${sits.notificationrecipient}").getOrElse(
		throw new IllegalStateException("sits.notificationrecipient property is missing")
	)

	@transient
	val templateLocation = "/WEB-INF/freemarker/emails/missed_monitoring_to_sits_email.ftl"

	override def verb: String = "view"

	override def title: String = "A department has uploaded missed monitoring points to SITS"

	override def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
		"agent" -> agent.getUserId,
		"departmentName" -> entities.head.fullName,
		"created" -> created.toString(DateFormats.NotificationDateTimePattern)
	))

	override def url: String = "https://warwick.ac.uk/tabula/manual/monitoring-points/upload-to-sits"

	override def urlTitle: String = "learn more about uploading missed monitoring points to SITS"

	override def recipient: User = userLookup.getUserByUserId(RecipientUsercode)

}
