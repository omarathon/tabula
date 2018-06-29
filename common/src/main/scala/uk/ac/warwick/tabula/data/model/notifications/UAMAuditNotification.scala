package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.{AcademicYear, DateFormats}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._


trait UAMAuditNotification extends Notification[Department, Unit] with MyWarwickNotification

case class DeptNameWithPermissionTreeUrl(departmentName: String, permissionTreeUrl: String)

object DeptNameWithPermissionTreeUrl {
	def permissionTreeUrl(departmentCode: String): String = s"/admin/permissions/department/$departmentCode/tree"

	def apply(department: Department): DeptNameWithPermissionTreeUrl = new DeptNameWithPermissionTreeUrl(department.fullName, permissionTreeUrl(department.code))
}

@Entity
@DiscriminatorValue("UAMAuditFirstNotification")
class UAMAuditFirstNotification extends UAMAuditNotification {

	@transient
	val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_email.ftl"

	@transient
	def permissionConfirmationDeadline: LocalDate = AcademicYear
		.forDate(this.created)
		.next
		.termsAndVacations
		.filter(_.isTerm)
		.head
		.firstDay
		.minusWeeks(1)

	def departments: Seq[Department] = entities

	def verb: String = "view"

	def title: String = s"Tabula Users Audit ${this.permissionConfirmationDeadline.getYear}"

	def url: String = "https://warwick.ac.uk/tabulaaudit"

	def urlTitle: String = "review and confirm user permissions"

	def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
		"departments" -> departments.flatMap { department =>
			Seq(
				Seq(DeptNameWithPermissionTreeUrl(department)),
				if (department.hasChildren) department.children.asScala.map(DeptNameWithPermissionTreeUrl.apply).toSeq else Seq.empty
			)
		}.flatten.distinct,
		"userAccessManager" -> agent.getFullName,
		"permissionConfirmation" -> permissionConfirmationDeadline.toString(DateFormats.NotificationDateOnlyPattern),
		"url" -> url,
		"urlTitle" -> urlTitle,
		"academicYear" -> s"${permissionConfirmationDeadline.minusYears(1).getYear}/${permissionConfirmationDeadline.getYear}"
	))

	@transient
	def recipients: Seq[User] = Seq(agent)
}

@Entity
@DiscriminatorValue("UAMAuditChaserNotification")
class UAMAuditSecondNotification extends UAMAuditFirstNotification {
	@transient
	override val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_second_email.ftl"
}
