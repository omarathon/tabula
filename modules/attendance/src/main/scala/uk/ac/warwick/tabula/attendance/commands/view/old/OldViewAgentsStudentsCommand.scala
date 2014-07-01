package uk.ac.warwick.tabula.attendance.commands.view.old

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.old.{BuildStudentPointsData, StudentPointsData}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object OldViewAgentsStudentsCommand {
	def apply(department: Department, agent: Member, relationshipType: StudentRelationshipType, academicYearOption: Option[AcademicYear]) =
		new OldViewAgentsStudentsCommand(department, agent, relationshipType, academicYearOption)
			with ViewAgentsStudentsPermissions
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringRelationshipServiceComponent
			with AutowiringUserLookupComponent
			with ComposableCommand[Seq[StudentPointsData]]
			with ViewAgentsStudentsState
			with ReadOnly with Unaudited
}

abstract class OldViewAgentsStudentsCommand(
	val department: Department,
	val agent: Member,
	val relationshipType: StudentRelationshipType,
	val academicYearOption: Option[AcademicYear]
)	extends CommandInternal[Seq[StudentPointsData]] with ViewAgentsStudentsState with BuildStudentPointsData {

	self: RelationshipServiceComponent =>

	def applyInternal() = {
		val students = relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember)
		buildData(students, academicYear)
	}
}

trait ViewAgentsStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewAgentsStudentsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}
}

trait ViewAgentsStudentsState {
	def department: Department
	def agent: Member
	def relationshipType: StudentRelationshipType
	def academicYearOption: Option[AcademicYear]

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}