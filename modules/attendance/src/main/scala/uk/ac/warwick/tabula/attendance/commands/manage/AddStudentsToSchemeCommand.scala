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
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.data.model.StudentMember

object AddStudentsToSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme) =
		new AddStudentsToSchemeCommandInternal(scheme)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with AddStudentsToSchemeValidation
			with AddStudentsToSchemeDescription
			with AddStudentsToSchemePermissions
			with AddStudentsToSchemeCommandState
			with SetStudents
}


class AddStudentsToSchemeCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: AddStudentsToSchemeCommandState with AttendanceMonitoringServiceComponent =>

	staticStudentIds = scheme.members.staticUserIds.asJava
	includedStudentIds = scheme.members.includedUserIds.asJava
	excludedStudentIds = scheme.members.excludedUserIds.asJava

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

	self: ProfileServiceComponent =>

	def scheme: AttendanceMonitoringScheme

	def membershipItems: Seq[SchemeMembershipItem] = {
		def getStudentMemberForUniversityId(entry: String): Option[StudentMember] =
			profileService.getMemberByUniversityId(entry) match {
				case Some(student: StudentMember) => Some(student)
				case _ => None
			}

		val staticMemberItems = (staticStudentIds.asScala diff excludedStudentIds.asScala diff includedStudentIds.asScala)
			.map(getStudentMemberForUniversityId).flatten.map(member => SchemeMembershipItem(member, StaticType))
		val includedMemberItems = includedStudentIds.asScala.map(getStudentMemberForUniversityId).flatten.map(member => SchemeMembershipItem(member, IncludeType))

		(staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.member.lastName, membershipItem.member.firstName))
	}

	// Bind variables

	// Students to persists
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = _

	// Students from Select page
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()
	var updatedStaticStudentIds: JList[String] = LazyLists.create()
	var updatedFilterQueryString: String = _
}
