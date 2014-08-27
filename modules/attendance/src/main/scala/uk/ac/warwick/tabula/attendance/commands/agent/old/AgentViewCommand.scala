package uk.ac.warwick.tabula.attendance.commands.agent.old

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.old.{BuildStudentPointsData, GroupMonitoringPointsByTerm, GroupedMonitoringPoint, StudentPointsData}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AgentViewCommand {
	def apply(agent: Member, relationshipType: StudentRelationshipType, academicYearOption: Option[AcademicYear]) =
		new AgentViewCommand(agent, relationshipType, academicYearOption)
			with AgentViewPermissions
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringRelationshipServiceComponent
			with AutowiringUserLookupComponent
			with ComposableCommand[(Seq[StudentPointsData], Map[String, Seq[GroupedMonitoringPoint]])]
			with AgentViewState
			with ReadOnly with Unaudited
}

abstract class AgentViewCommand(val agent: Member, val relationshipType: StudentRelationshipType, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[(Seq[StudentPointsData], Map[String, Seq[GroupedMonitoringPoint]])] with AgentViewState
	with BuildStudentPointsData with GroupMonitoringPointsByTerm with TaskBenchmarking {

	self: RelationshipServiceComponent =>

	def applyInternal() = {
		val students = benchmarkTask("Get relationships with current user") {
			relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember)
		}
		val studentPointsData = benchmarkTask("Build student data") { buildData(students, academicYear) }
		val groupedPoints = benchmarkTask("Group similar points") { groupSimilarPointsByTerm(
			studentPointsData.flatMap(s =>
				s.pointsByTerm.values.flatMap{map => map.keys}),
			Seq(),
			academicYear
		)}
		(studentPointsData, groupedPoints)
	}
}

trait AgentViewPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AgentViewState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), agent)
	}
}

trait AgentViewState {
	def agent: Member
	def relationshipType: StudentRelationshipType
	def academicYearOption: Option[AcademicYear]

	val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}