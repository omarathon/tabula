package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands.{FiltersRelationships, CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.permissions.Permissions
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.{AutowiringSitsStatusDaoComponent, AutowiringModeOfAttendanceDaoComponent}


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
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringModeOfAttendanceDaoComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSitsStatusDaoComponent
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
	
	lazy val allCourses =
		profileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, currentMember, Nil)
			.flatMap(student => Option(student.mostSignificantCourse))
	def allDepartments = allCourses.map(_.department).distinct
	def allRoutes = allCourses.map(_.route).distinct
}

abstract class ViewRelatedStudentsCommandInternal(val currentMember: Member, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[StudentMember]] with TaskBenchmarking with ViewRelatedStudentsCommandState with BindListener {
	self: ProfileServiceComponent =>

	def applyInternal(): Seq[StudentMember] =  {
		profileService.getStudentsByAgentRelationshipAndRestrictions(relationshipType, currentMember, buildRestrictions())
	}

	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (sprStatuses.isEmpty) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
	}
}

trait ViewRelatedStudentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self:ViewRelatedStudentsCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), currentMember)
	}
}
