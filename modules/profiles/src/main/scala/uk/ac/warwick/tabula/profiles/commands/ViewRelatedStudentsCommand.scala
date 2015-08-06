package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
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


object ViewRelatedStudentsCommand{
	def apply(currentMember: Member, relationshipType: StudentRelationshipType): Command[Seq[StudentCourseDetails]] = {
		new ViewRelatedStudentsCommandInternal(currentMember, relationshipType)
			with ComposableCommand[Seq[StudentCourseDetails]]
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

	lazy val allCourses =
		profileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, currentMember, Nil)
	lazy val allDepartments = allCourses.flatMap(c => Option(c.department)).distinct
	lazy val allRoutes = allCourses.flatMap(c => Option(c.route)).distinct
}

abstract class ViewRelatedStudentsCommandInternal(val currentMember: Member, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[StudentCourseDetails]] with TaskBenchmarking with ViewRelatedStudentsCommandState with BindListener {
	self: ProfileServiceComponent =>

	def applyInternal(): Seq[StudentCourseDetails] =  {
		val year = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		profileService.getSCDsByAgentRelationshipAndRestrictions(relationshipType, currentMember, buildRestrictions(year))
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
