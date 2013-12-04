package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class RouteAuditor(route: model.Route) extends BuiltInRole(RouteAuditorRoleDefinition, route)

case object RouteAuditorRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Route Auditor"

	GrantsScopedPermission( 
		MonitoringPoints.View,
		
		Profiles.Read.Core,
		Profiles.Read.NextOfKin,
	  Profiles.Read.Timetable,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.Search,
		
		Profiles.MeetingRecord.Read(PermissionsSelector.Any[StudentRelationshipType]),
		
		MemberNotes.Read,
		
		Profiles.Read.SmallGroups
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = false
}