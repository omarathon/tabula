package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import collection.JavaConverters._

abstract class MembershipItemType(val value: String)
case object StaticType extends MembershipItemType("static")
case object IncludeType extends MembershipItemType("include")
case object ExcludeType extends MembershipItemType("exclude")

/** 
 * Item in list of members for displaying in view.
	*/
case class MembershipItem(
	member: StudentMember,
	itemType: MembershipItemType // static, include or exclude
) {
	def itemTypeString = itemType.value
}

case class EditSchemeMembershipCommandResult(
	membershipItems: Seq[MembershipItem],
	missingMembers: Seq[String]
)

object EditSchemeMembershipCommand {
	def apply(AttendanceMonitoringScheme: AttendanceMonitoringScheme) =
		new EditSchemeMembershipCommandInternal(AttendanceMonitoringScheme)
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[EditSchemeMembershipCommandResult]
			with EditSchemeMembershipPermissions
			with EditSchemeMembershipCommandState
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditSchemeMembershipCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[EditSchemeMembershipCommandResult] {

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

		val missingMembers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq

		val validMembers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq
		val newStatic = staticStudentIds.asScala.toSeq diff validMembers.map(_.universityId)
		val newExcluded = excludedStudentIds.asScala.toSeq diff validMembers.map(_.universityId)
		val newIncluded = (includedStudentIds.asScala.toSeq ++ validMembers.map(_.universityId)).distinct

		val membershipItems = (newStatic.map(getStudentMemberForString).flatten.map{ member => MembershipItem(member, StaticType)} ++
			newExcluded.map(getStudentMemberForString).flatten.map{ member => MembershipItem(member, ExcludeType)} ++
			newIncluded.map(getStudentMemberForString).flatten.map{ member => MembershipItem(member, IncludeType)}
		).sortBy(membershipItem => (membershipItem.member.lastName, membershipItem.member.firstName))

		EditSchemeMembershipCommandResult(membershipItems, missingMembers)
	}

}


trait EditSchemeMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditSchemeMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait EditSchemeMembershipCommandState extends AddStudentsToSchemeCommandState{
	// Extra bind variables
	var massAddUsers: String = _
	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

}
