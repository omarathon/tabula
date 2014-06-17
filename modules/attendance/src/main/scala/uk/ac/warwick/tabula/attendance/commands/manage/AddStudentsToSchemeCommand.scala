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
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.data.{SchemeMembershipIncludeType, SchemeMembershipStaticType, SchemeMembershipItem}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember

object AddStudentsToSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new AddStudentsToSchemeCommandInternal(scheme, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with PopulateAddStudentsToSchemeCommandInternal
			with AddStudentsToSchemeValidation
			with AddStudentsToSchemeDescription
			with AddStudentsToSchemePermissions
			with AddStudentsToSchemeCommandState
			with SetStudents
}


class AddStudentsToSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[AttendanceMonitoringScheme] with TaskBenchmarking {

	self: AddStudentsToSchemeCommandState with AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	override def applyInternal() = {
		scheme.members.staticUserIds = staticStudentIds.asScala
		scheme.members.includedUserIds = includedStudentIds.asScala
		scheme.members.excludedUserIds = excludedStudentIds.asScala
		scheme.memberQuery = filterQueryString
		scheme.updatedDate = DateTime.now
		attendanceMonitoringService.saveOrUpdate(scheme)
		benchmark("updateCheckpointTotals") {
			profileService.getAllMembersWithUniversityIds(scheme.members.members).map {
				case student: StudentMember => attendanceMonitoringService.updateCheckpointTotal(student, scheme.department, scheme.academicYear)
				case _ =>
			}
		}
		scheme
	}

}

trait PopulateAddStudentsToSchemeCommandInternal extends PopulateOnForm {

	self: AddStudentsToSchemeCommandState =>

	override def populate() = {
		staticStudentIds = scheme.members.staticUserIds.asJava
		includedStudentIds = scheme.members.includedUserIds.asJava
		excludedStudentIds = scheme.members.excludedUserIds.asJava
		filterQueryString = scheme.memberQuery
	}
}

trait SetStudents {

	self: AddStudentsToSchemeCommandState =>

	def linkToSits() = {
		includedStudentIds.clear()
		includedStudentIds.addAll(updatedIncludedStudentIds)
		excludedStudentIds.clear()
		excludedStudentIds.addAll(updatedExcludedStudentIds)
		staticStudentIds.clear()
		staticStudentIds.addAll(updatedStaticStudentIds)
		filterQueryString = updatedFilterQueryString
	}

	def importAsList() = {
		includedStudentIds.clear()
		excludedStudentIds.clear()
		staticStudentIds.clear()
		val newList = ((updatedStaticStudentIds.asScala diff updatedExcludedStudentIds.asScala) ++ updatedIncludedStudentIds.asScala).distinct
		includedStudentIds.addAll(newList.asJava)
		filterQueryString = ""
	}

}

trait AddStudentsToSchemeValidation extends SelfValidating with TaskBenchmarking {

	self: AddStudentsToSchemeCommandState with ProfileServiceComponent with SecurityServiceComponent =>

	override def validate(errors: Errors) {
		// In practice there should be no students that fail this validation
		// but this protects against hand-rolled POSTs
		val members = benchmark("profileService.getAllMembersWithUniversityIds") {
			profileService.getAllMembersWithUniversityIds(
				((staticStudentIds.asScala
					diff excludedStudentIds.asScala)
					diff includedStudentIds.asScala)
					++ includedStudentIds.asScala
			)
		}
		val noPermissionMembers = benchmark("noPermissionMembers") {
			members.filter(!securityService.can(user, Permissions.MonitoringPoints.Manage, _))
		}
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

	self: AttendanceMonitoringServiceComponent =>

	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

	def membershipItems: Seq[SchemeMembershipItem] = {
		val staticMemberItems = attendanceMonitoringService.findSchemeMembershipItems(
			(staticStudentIds.asScala diff excludedStudentIds.asScala) diff includedStudentIds.asScala,
			SchemeMembershipStaticType
		)
		val includedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(includedStudentIds.asScala, SchemeMembershipIncludeType)

		(staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
	}

	// Bind variables

	// Students to persists
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""

	// Students from Select page
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()
	var updatedStaticStudentIds: JList[String] = LazyLists.create()
	var updatedFilterQueryString: String = ""
}
