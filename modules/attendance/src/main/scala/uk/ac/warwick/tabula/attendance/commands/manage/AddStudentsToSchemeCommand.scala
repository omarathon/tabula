package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object AddStudentsToSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme) =
		new AddStudentsToSchemeCommandInternal(scheme)
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with AddStudentsToSchemeValidation
			with AddStudentsToSchemeDescription
			with AddStudentsToSchemePermissions
			with AddStudentsToSchemeCommandState
}


class AddStudentsToSchemeCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: AddStudentsToSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		scheme.members.staticUserIds = staticStudentIds.asScala
		scheme.members.includedUserIds = includedStudentIds.asScala
		scheme.members.excludedUserIds = excludedStudentIds.asScala
		scheme.memberQuery = filterQueryString
		scheme.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(scheme)
		scheme
	}

}

trait AddStudentsToSchemeValidation extends SelfValidating {

	self: AddStudentsToSchemeCommandState =>

	override def validate(errors: Errors) {

	}

}

trait AddStudentsToSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddStudentsToSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait AddStudentsToSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: AddStudentsToSchemeCommandState =>

	override lazy val eventName = "AddStudentsToScheme"

	override def describe(d: Description) {
		d.attendanceMonitoringScheme(scheme)
	}
}

trait AddStudentsToSchemeCommandState {
	def scheme: AttendanceMonitoringScheme

	// Bind variables
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = _
}
