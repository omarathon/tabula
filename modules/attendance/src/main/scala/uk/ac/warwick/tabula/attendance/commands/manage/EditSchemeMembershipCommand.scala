package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import scala.Some
import uk.ac.warwick.tabula.helpers.LazyLists

object EditSchemeMembershipCommand {
	def apply(AttendanceMonitoringScheme: AttendanceMonitoringScheme) =
		new EditSchemeMembershipCommandInternal(AttendanceMonitoringScheme)
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with PopulateEditSchemeMembershipCommand
			with EditSchemeMembershipPermissions
			with EditSchemeMembershipCommandState
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditSchemeMembershipCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: EditSchemeMembershipCommandState with UserLookupComponent with ProfileServiceComponent =>

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

		scheme
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

	def membershipItems: Seq[SchemeMembershipItem] = {
		def getStudentMemberForUniversityId(entry: String): Option[StudentMember] =
			profileService.getMemberByUniversityId(entry) match {
				case Some(student: StudentMember) => Some(student)
				case _ => None
			}

		val excludedMemberItems = updatedExcludedStudentIds.asScala.map(getStudentMemberForUniversityId).flatten.map(member => SchemeMembershipItem(member, ExcludeType))
		val includedMemberItems = updatedIncludedStudentIds.asScala.map(getStudentMemberForUniversityId).flatten.map(member => SchemeMembershipItem(member, IncludeType))

		(excludedMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.member.lastName, membershipItem.member.firstName))
	}

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
