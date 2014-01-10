package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.TaskBenchmarking

object AgentViewCommand {
	def apply(agent: Member, relationshipType: StudentRelationshipType, academicYearOption: Option[AcademicYear]) =
		new AgentViewCommand(agent, relationshipType, academicYearOption)
			with AgentViewPermissions
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringRelationshipServiceComponent
			with ComposableCommand[Seq[StudentPointsData]]
			with AgentViewState
			with ReadOnly with Unaudited
}

abstract class AgentViewCommand(val agent: Member, val relationshipType: StudentRelationshipType, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[Seq[StudentPointsData]] with AgentViewState with BuildStudentPointsData with TaskBenchmarking {
	self: RelationshipServiceComponent =>

	def applyInternal() = {
		val students = benchmarkTask("Get relationships with current user") { relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember) }
		benchmarkTask("Build data") { buildData(students, academicYear) }
	}
}

trait AgentViewPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AgentViewState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), agent)
	}
}

trait AgentViewState {
	def agent: Member
	def relationshipType: StudentRelationshipType
	def academicYearOption: Option[AcademicYear]

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}