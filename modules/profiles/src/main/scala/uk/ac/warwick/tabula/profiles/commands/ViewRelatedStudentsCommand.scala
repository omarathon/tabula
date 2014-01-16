package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands.{FiltersRelationships, CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.permissions.Permissions
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.commands.TaskBenchmarking


// Don't need this, unless there is specific state on the command which the controller needs access to.
//
//trait ViewRelatedStudentsCommand extends ComposableCommand[Seq[StudentRelationship]]  {
//	this:ViewRelatedStudentsCommandInternal=>
//}
object ViewRelatedStudentsCommand{
	def apply(currentMember: Member, relationshipType: StudentRelationshipType): Command[Seq[StudentMember]] = {
		new ViewRelatedStudentsCommandInternal(currentMember, relationshipType)
			with ComposableCommand[Seq[StudentMember]]
			with AutowiringProfileServiceComponent
			with ViewRelatedStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}


trait ViewRelatedStudentsCommandState extends FiltersRelationships {
	self: ProfileServiceComponent =>
	
	val currentMember: Member
	val relationshipType: StudentRelationshipType

	var studentsPerPage = FiltersRelationships.DefaultStudentsPerPage
	var page = 1

	var departments: JList[Department] = JArrayList()
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
	
	lazy val allCourses = profileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, currentMember, Nil).flatMap(student => Option(student.mostSignificantCourse))
	def allDepartments = allCourses.map(_.department).distinct
	def allRoutes = allCourses.map(_.route).distinct
}

abstract class ViewRelatedStudentsCommandInternal(val currentMember: Member, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[StudentMember]] with TaskBenchmarking with ViewRelatedStudentsCommandState {
	self: ProfileServiceComponent =>

	def applyInternal(): Seq[StudentMember] =  {
		profileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, currentMember, buildRestrictions())
	}
}

trait ViewRelatedStudentsCommandPermissions extends RequiresPermissionsChecking {
	self:ViewRelatedStudentsCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), currentMember)
	}
}
