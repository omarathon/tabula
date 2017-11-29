package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.{AcademicOfficeMembershipNotification, ManualMembershipWarningNotification}
import uk.ac.warwick.tabula.data.model.{Department, DepartmentWithManualUsers, Notification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions


object ManualMembershipWarningCommand {
	def apply() = new ManualMembershipWarningCommandInternal()
		with ComposableCommand[Seq[DepartmentWithManualUsers]]
		with PubliclyVisiblePermissions with ReadOnly with Unaudited
		with AutowiringAssessmentMembershipServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
}

object ManualMembershipWarningCommandWithNotification {
	def apply() = new ManualMembershipWarningCommandInternal()
		with ComposableCommand[Seq[DepartmentWithManualUsers]]
		with PubliclyVisiblePermissions with ReadOnly with Unaudited
		with AutowiringAssessmentMembershipServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with ManualMembershipWarningNotifications
}

abstract class ManualMembershipWarningCommandInternal() extends CommandInternal[Seq[DepartmentWithManualUsers]] with Logging {

	self: AssessmentMembershipServiceComponent =>

	def applyInternal(): Seq[DepartmentWithManualUsers] = {
		benchmark("ManualMembershipWarning") {
			val currentSITSAcademicYear = AcademicYear.now()
			assessmentMembershipService.departmentsWithManualAssessmentsOrGroups(currentSITSAcademicYear)
		}
	}
}

trait ManualMembershipWarningNotifications extends Notifies[Seq[DepartmentWithManualUsers], Department] {

	self: ModuleAndDepartmentServiceComponent =>

	def emit(infos: Seq[DepartmentWithManualUsers]): Seq[Notification[Department, Unit]] = {
		val departments = (for (info <- infos; dept <- moduleAndDepartmentService.getDepartmentById(info.department)) yield dept -> info).toMap

		val deptNotifications = departments.map{ case (department, info) =>
			Notification.init(new ManualMembershipWarningNotification, null, department).tap(n => {
				n.numAssignments = info.assignments
				n.numSmallGroupSets = info.smallGroupSets
			})
		}.toSeq

		val eoNotification = Notification.init(new AcademicOfficeMembershipNotification, null, departments.keys.toSeq)

		deptNotifications :+ eoNotification
	}
}

