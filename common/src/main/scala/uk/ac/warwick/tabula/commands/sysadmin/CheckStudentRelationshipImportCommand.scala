package uk.ac.warwick.tabula.commands.sysadmin

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, MemberDaoComponent}
import uk.ac.warwick.tabula.services.UserLookupService.{UniversityId, Usercode}
import uk.ac.warwick.tabula.services.scheduling.ProfileImporter.GetSingleStudentInformation
import uk.ac.warwick.tabula.services.scheduling.SupervisorImporter.SupervisorMappingDebugQuery
import uk.ac.warwick.tabula.services.scheduling.SupervisorImportDebugRow
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import TutorSource._

case class CheckStudentRelationshipImport(
	isStudent: Boolean,
	isUpstreamStudent: Boolean,
	isCurrentStudent: Boolean,
	isFreshCourse: Boolean,
	studentUser: User,
	department: Option[Department],
	relationships: Seq[RelationshipTypeCheck],
	personalTutorCheck: PersonalTutorCheck
)

object TutorSource {
	sealed class TutorSource(val value: String)
	object SCJ extends TutorSource(value = "SCJ")
	object SPR extends TutorSource(value = "SPR")
}


case class PersonalTutorCheck(
	sitsIsSource: Boolean,
	fieldUsed: TutorSource,
	isTaught: Boolean, // we don't expect PGRs to have a personal tutor
	isMember: Boolean,
	tutorId: UniversityId,
	rawData: Option[PersonalTutorDebugRow]
)

case class PersonalTutorDebugRow(
	courseCode: String,
	scjTutor: String,
	sprTutor: String
)

case class RelationshipTypeCheck(
	sitsIsSource: Boolean,
	relationshipType: StudentRelationshipType,
	rows: Seq[RelationshipRow]
)

case class RelationshipRow(
	isForCurrentCourse: Boolean,
	hasPersonnelRecord: Boolean,
	isMember: Boolean,
	isUpstreamMember: Boolean,
	rawData: SupervisorImportDebugRow
) {
	def canImport: Boolean = isForCurrentCourse && hasPersonnelRecord && isMember && isUpstreamMember
}

object CheckStudentRelationshipImportCommand {
	def apply() =
		new CheckStudentRelationshipImportCommandInternal
			with ComposableCommand[CheckStudentRelationshipImport]
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringMemberDaoComponent
			with AutowiringUserLookupComponent
			with CheckStudentRelationshipImportCommandPermissions
			with CheckStudentRelationshipImportCommandState
			with Unaudited
}

class CheckStudentRelationshipImportCommandInternal extends CommandInternal[CheckStudentRelationshipImport] {
	self: CheckStudentRelationshipImportCommandState with RelationshipServiceComponent with ProfileServiceComponent with UserLookupComponent
		with MemberDaoComponent =>

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	class TutorQuery(ds: DataSource) extends MappingSqlQuery[PersonalTutorDebugRow](ds, GetSingleStudentInformation) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): PersonalTutorDebugRow = PersonalTutorDebugRow(
			rs.getString("course_code"),
			rs.getString("scj_tutor1"),
			rs.getString("spr_tutor1")
		)
	}


	override def applyInternal(): CheckStudentRelationshipImport = {
		lazy val query = new SupervisorMappingDebugQuery(sits)
		lazy val tutorQuery = new TutorQuery(sits)

		val studentUser = userLookup.getUserByUserId(student)
		val studentMember = profileService.getMemberByUser(studentUser, disableFilter = true).collect { case sm: StudentMember => sm }
		val mostSignificantCourse = studentMember.flatMap(_.mostSignificantCourseDetails)
		val department = mostSignificantCourse.flatMap(c => Option(c.department))

		val personalTutorCheck =
			studentMember.map { sm =>
				val data = tutorQuery.executeByNamedParam(Map("universityId" -> sm.universityId).asJava).asScala.headOption

				val sitsIsSource = (for (
					tutorType <- relationshipService.getStudentRelationshipTypeByUrlPart("tutor");
					dept <- department
				) yield dept.getStudentRelationshipSource(tutorType) == StudentRelationshipSource.SITS).getOrElse(false)

				val (tutorSource, tutorId) = department match {
					case Some(d) if sitsIsSource && d.code.toLowerCase == "wm" => (SCJ, data.map(_.scjTutor).map(_.substring(2)))
					case Some(d) if sitsIsSource => (SPR, data.map(_.sprTutor))
					case _ => (SPR, None)
				}

				val tutorMember = tutorId.flatMap(memberDao.getByUniversityIdStaleOrFresh)

				PersonalTutorCheck(
					sitsIsSource = sitsIsSource,
					fieldUsed = tutorSource,
					isTaught = data.exists(d => CourseType.fromCourseCode(d.courseCode) != CourseType.PGR),
					isMember = tutorMember.isDefined,
					tutorId = tutorId.orNull,
					rawData = data
				)
			}.getOrElse(PersonalTutorCheck(sitsIsSource = false, SCJ, isTaught = false, isMember = false, null, None))

		val relationships = relationshipService.getStudentRelationshipTypesWithRdxType.map { relationshipType =>
			val rows = if(studentUser.isFoundUser && studentUser.getWarwickId.hasText) {
				query.executeByNamedParam(Map("scj_code" -> s"${studentUser.getWarwickId}%", "sits_examiner_type" -> relationshipType.defaultRdxType).asJava).asScala
			} else {
				Seq()
			}

			val sitsIsSource = department
				.map(_.getStudentRelationshipSource(relationshipType))
				.getOrElse(relationshipType.defaultSource) == StudentRelationshipSource.SITS

			RelationshipTypeCheck(
				sitsIsSource = sitsIsSource,
				relationshipType = relationshipType,
				rows = rows.map { row =>
					val agentMember = profileService.getMemberByUniversityIdStaleOrFresh(row.agentId)
					RelationshipRow(
						isForCurrentCourse = mostSignificantCourse.exists(_.scjCode == row.scjCode),
						hasPersonnelRecord = row.agentId != null,
						isMember = agentMember.isDefined,
						isUpstreamMember = agentMember.exists(m => !m.stale),
						rawData = row
					)
				}
			)
		}

		CheckStudentRelationshipImport(
			isStudent = studentMember.isDefined,
			isUpstreamStudent = studentMember.exists(m => !m.stale),
			isCurrentStudent = !studentMember.exists(_.permanentlyWithdrawn),
			isFreshCourse = mostSignificantCourse.isDefined,
			studentUser = studentUser,
			department = department,
			relationships = relationships,
			personalTutorCheck = personalTutorCheck
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