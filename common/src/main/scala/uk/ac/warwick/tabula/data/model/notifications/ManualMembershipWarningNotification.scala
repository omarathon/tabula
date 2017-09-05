package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ModuleAndDepartmentService}
import uk.ac.warwick.userlookup.User


object ManualMembershipWarningNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/manual_membership_warning.ftl"
}

@Entity
@DiscriminatorValue("ManualMembershipWarning")
class ManualMembershipWarningNotification extends Notification[Department, Unit]
	with SingleItemNotification[Department]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	priority = Warning

	def department: Department = item.entity

	def numAssignments: Int = getIntSetting("numAssignments", default=0)
	def numAssignments_= (count:Int) { settings += ("numAssignments" -> count) }

	def numSmallGroupSets: Int = getIntSetting("numSmallGroupSets", default=0)
	def numSmallGroupSets_= (count:Int) { settings += ("numSmallGroupSets" -> count) }

	def verb = "view"
	def title: String = s"Some assignments or small group sets in ${department.name} have manually added students."
	def url: String = Routes.department.manualMembership(department)
	def urlTitle = s"view a list of assignments and small group sets with manually added students for ${department.name}"

	def content = FreemarkerModel(ManualMembershipWarningNotification.templateLocation, Map (
		"department" -> department,
		"numAssignments" -> numAssignments,
		"numSmallGroupSets" -> numSmallGroupSets
	))

	@transient
	override def recipients: Seq[User] =
	// department.owners is not populated correctly if department not fetched directly
		moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
}
