package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import scala.Some
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.data.{SchemeMembershipIncludeType, SchemeMembershipExcludeType, SchemeMembershipItem}

case class EditSchemeMembershipCommandResult(
	updatedIncludedStudentIds: JList[String],
	updatedExcludedStudentIds: JList[String],
	membershipItems: Seq[SchemeMembershipItem]
)

object EditSchemeMembershipCommand {
	def apply(AttendanceMonitoringScheme: AttendanceMonitoringScheme) =
		new EditSchemeMembershipCommandInternal(AttendanceMonitoringScheme)
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[EditSchemeMembershipCommandResult]
			with PopulateEditSchemeMembershipCommand
			with EditSchemeMembershipPermissions
			with EditSchemeMembershipCommandState
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditSchemeMembershipCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[EditSchemeMembershipCommandResult] {

	self: EditSchemeMembershipCommandState with UserLookupComponent with ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		def getStudentMemberForString(entry: String): Option[StudentMember] = {
			if (UniversityId.isValid(entry)) {
				profileService.getMemberByUniversityId(entry) match {
					case Some(student: StudentMember) => Some(student)
					case _ => None
				}
			} else {
				val user = userLookup.getUserByUserId(entry)
				if (user.isFoundUser) {
					profileService.getMemberByUser(user) match {
						case Some(student: StudentMember) => Some(student)
						case _ => None
					}
				} else {
					None
				}
			}
		}

		val massAddedUserMap: Map[String, Option[StudentMember]] = massAddUsersEntries.map{ entry =>
			entry -> getStudentMemberForString(entry)
		}.toMap

		missingMembers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq

		val validMembers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq
		updatedIncludedStudentIds = (updatedIncludedStudentIds.asScala.toSeq ++ validMembers.map(_.universityId)).asJava
		updatedExcludedStudentIds = (updatedExcludedStudentIds.asScala.toSeq diff updatedIncludedStudentIds.asScala.toSeq).asJava

		// Users processed, so reset fields
		massAddUsers = ""

		val membershipItems: Seq[SchemeMembershipItem] = {
			val excludedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(updatedExcludedStudentIds.asScala, SchemeMembershipExcludeType)
			val includedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(updatedIncludedStudentIds.asScala, SchemeMembershipIncludeType)
			(excludedMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
		}

		EditSchemeMembershipCommandResult(updatedIncludedStudentIds, updatedExcludedStudentIds, membershipItems)
	}

}

trait PopulateEditSchemeMembershipCommand extends PopulateOnForm {

	self: EditSchemeMembershipCommandState =>

	override def populate() = {
		updatedIncludedStudentIds = includedStudentIds
		updatedExcludedStudentIds = excludedStudentIds
	}

}


trait EditSchemeMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditSchemeMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait EditSchemeMembershipCommandState {

	self: ProfileServiceComponent =>

	def scheme: AttendanceMonitoringScheme

	// Bind variables

	// Store original students for reset
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()

	// Store updated students
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()

	var massAddUsers: String = _
	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

	// Not bound
	var missingMembers: Seq[String] = _

}
