package uk.ac.warwick.tabula.commands.sysadmin

import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.services.UserLookupService.Usercode
import uk.ac.warwick.tabula.services.scheduling.{SupervisorImportDebugRow, SupervisorImporter}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

case class CheckStudentRelationshipImport (
	isStudent: Boolean,
	isUpstreamStudent: Boolean,
	isCurrentStudent: Boolean,
	isFreshCourse: Boolean,
	studentUser: User,
	department: Option[Department],
	relationships: Seq[RelationshipTypeCheck]
)

case class RelationshipTypeCheck(
	sitsIsSource: Boolean,
	relationshipType: StudentRelationshipType,
	rows: Seq[RelationshipRow]
)

case class RelationshipRow (
	isForCurrentCourse: Boolean,
	hasPersonnelRecord: Boolean,
	isMember: Boolean,
	isUpstreamMember: Boolean,
	rawData: SupervisorImportDebugRow,
) {
	def canImport: Boolean = isForCurrentCourse && hasPersonnelRecord && isMember && isUpstreamMember
}

object CheckStudentRelationshipImportCommand {
	def apply() =
		new CheckStudentRelationshipImportCommandInternal
			with ComposableCommand[CheckStudentRelationshipImport]
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringUserLookupComponent
			with CheckStudentRelationshipImportCommandPermissions
			with CheckStudentRelationshipImportCommandState
			with Unaudited
}

class CheckStudentRelationshipImportCommandInternal extends CommandInternal[CheckStudentRelationshipImport] {
	self: CheckStudentRelationshipImportCommandState with RelationshipServiceComponent with ProfileServiceComponent with UserLookupComponent =>

	import SupervisorImporter._
	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	override def applyInternal(): CheckStudentRelationshipImport = {
		lazy val query = new SupervisorMappingDebugQuery(sits)
		val studentUser = userLookup.getUserByUserId(student)
		val studentMember = profileService.getMemberByUser(studentUser, disableFilter = true).collect { case sm: StudentMember => sm }
		val mostSignificantCourse = studentMember.flatMap(_.mostSignificantCourseDetails)

		CheckStudentRelationshipImport(
			isStudent = studentMember.isDefined,
			isUpstreamStudent = studentMember.exists(_.isFresh),
			isCurrentStudent = !studentMember.exists(_.permanentlyWithdrawn),
			isFreshCourse = mostSignificantCourse.isDefined,
			studentUser = studentUser,
			department = mostSignificantCourse.flatMap(c => Option(c.department)),
			relationships =
				relationshipService.getStudentRelationshipTypesWithRdxType.map { relationshipType =>
					val rows = query.executeByNamedParam(Map("scj_code" -> s"${studentUser.getWarwickId}%", "sits_examiner_type" -> relationshipType.defaultRdxType).asJava).asScala

					val sitsIsSource = mostSignificantCourse.exists(msc =>
						StudentRelationshipSource.SITS == Option(msc.department)
							.map(_.getStudentRelationshipSource(relationshipType))
							.getOrElse(relationshipType.defaultSource)
						)

					RelationshipTypeCheck(
						sitsIsSource = sitsIsSource,
						relationshipType = relationshipType,
						rows = rows.map { row =>
							val agentMember = profileService.getMemberByUniversityIdStaleOrFresh(row.agentId)
							RelationshipRow(
								isForCurrentCourse = mostSignificantCourse.exists(_.scjCode == row.scjCode),
								hasPersonnelRecord = row.agentId != null,
								isMember = agentMember.isDefined,
								isUpstreamMember = agentMember.exists(_.isFresh),
								rawData = row
							)
						}
					)
				}
		)
	}
}

trait CheckStudentRelationshipImportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CheckStudentRelationshipImportCommandState =>

	def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.StudentRelationshipType.Manage)
	}
}

trait CheckStudentRelationshipImportCommandState {
	var student: Usercode = _
}