package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.CurrentUser

object EditSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new EditSchemeCommandInternal(scheme, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with PopulateEditSchemeCommandInternal
			with EditSchemeValidation
			with EditSchemeDescription
			with EditSchemePermissions
			with EditSchemeCommandState
			with EditSetStudents
}


class EditSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: EditSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		scheme.name = name
		scheme.pointStyle = pointStyle
		scheme.members.staticUserIds = staticStudentIds.asScala
		scheme.members.includedUserIds = includedStudentIds.asScala
		scheme.members.excludedUserIds = excludedStudentIds.asScala
		scheme.memberQuery = filterQueryString
		scheme.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(scheme)
		scheme
	}

}

trait PopulateEditSchemeCommandInternal extends PopulateOnForm {

	self: EditSchemeCommandState =>

	override def populate() = {
		name = scheme.name
		pointStyle = scheme.pointStyle
		staticStudentIds = scheme.members.staticUserIds.asJava
		includedStudentIds = scheme.members.includedUserIds.asJava
		excludedStudentIds = scheme.members.excludedUserIds.asJava
		filterQueryString = scheme.memberQuery
	}
}

trait EditSetStudents extends SetStudents {

	self: EditSchemeCommandState =>

	override def linkToSits() = {
		name = scheme.name
		pointStyle = scheme.pointStyle
		super.linkToSits()
	}

	override def importAsList() = {
		name = scheme.name
		pointStyle = scheme.pointStyle
		super.importAsList()
	}

}

trait EditSchemeValidation extends SelfValidating {

	self: EditSchemeCommandState with ProfileServiceComponent with SecurityServiceComponent =>

	override def validate(errors: Errors) {

		if (!scheme.points.isEmpty && pointStyle != scheme.pointStyle) {
			errors.rejectValue("pointStyle", "attendanceMonitoringScheme.pointStyle.pointsExist")
		}

		// In practice there should be no students that fail this validation
		// but this protects against hand-rolled POSTs
		val members = profileService.getAllMembersWithUniversityIds(
			((staticStudentIds.asScala
				diff excludedStudentIds.asScala)
				diff includedStudentIds.asScala)
				++ includedStudentIds.asScala
		)
		val noPermissionMembers = members.filter(!securityService.can(user, Permissions.MonitoringPoints.Manage, _))
		if (!noPermissionMembers.isEmpty) {
			errors.rejectValue(
				"staticStudentIds",
				"attendanceMonitoringScheme.student.noPermission",
				Array(noPermissionMembers.map(_.universityId).mkString(", ")),
				"You do not have permission to manage these students' attendance"
			)
		}
	}

}

trait EditSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait EditSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: EditSchemeCommandState =>

	override lazy val eventName = "EditScheme"

	override def describe(d: Description) {
		d.attendanceMonitoringScheme(scheme)
	}
}

trait EditSchemeCommandState extends AddStudentsToSchemeCommandState {

	self: AttendanceMonitoringServiceComponent =>

	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	// Bind variables
	var name: String = _
	var pointStyle: AttendanceMonitoringPointStyle = _
}
