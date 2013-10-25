package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import FilterStudentsCommand._
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.SitsStatus
import org.hibernate.criterion.Criterion
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.ScalaRestriction._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.DegreeType
import uk.ac.warwick.tabula.data.model.CourseType
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

case class FilterStudentsResults(
	students: Seq[StudentMember],
	totalResults: Int
)

object FilterStudentsCommand {
	def apply(department: Department) =
		new FilterStudentsCommand(department)
			with ComposableCommand[FilterStudentsResults]
			with FilterStudentsPermissions
			with AutowiringProfileServiceComponent
			with ReadOnly with Unaudited
			
	val MaxStudentsPerPage = 100
	val DefaultStudentsPerPage = 50
}

class FilterStudentsCommand(val department: Department) extends CommandInternal[FilterStudentsResults] with FilterStudentsState {
	self: ProfileServiceComponent =>
		
	// Add all non-withdrawn codes to SPR statuses by default
	if (sprStatuses.isEmpty()) {
		allSprStatuses.filter { status => !status.code.startsWith("P") && status.code != "T" }.foreach { sprStatuses.add }
	}
	
	def applyInternal() = {
		val totalResults = profileService.countStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions()
		)
		
		val students = profileService.findStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = order, 
			maxResults = studentsPerPage, 
			startResult = (studentsPerPage * (page-1))
		)
		
		FilterStudentsResults(students, totalResults)
	}
	
	private def buildRestrictions(): Seq[ScalaRestriction] = 
		Seq(
			// Course type
			startsWithIfNotEmpty(
				"course.code", courseTypes.asScala.map { _.courseCodeChar.toString }, 
				"mostSignificantCourse" -> "studentCourseDetails",
				"studentCourseDetails.course" -> "course"
			),
				
			// Route
			inIfNotEmpty(
				"studentCourseDetails.route", routes.asScala, 
				"mostSignificantCourse" -> "studentCourseDetails"
			),
			
			// Mode of attendance
			inIfNotEmpty(
				"studentCourseYearDetails.modeOfAttendance", modesOfAttendance.asScala, 
				"mostSignificantCourse" -> "studentCourseDetails",
				"studentCourseDetails.studentCourseYearDetails" -> "studentCourseYearDetails"
			),
			
			// Year of study
			inIfNotEmpty(
				"studentCourseYearDetails.yearOfStudy", yearsOfStudy.asScala, 
				"mostSignificantCourse" -> "studentCourseDetails",
				"studentCourseDetails.studentCourseYearDetails" -> "studentCourseYearDetails"
			),
			
			// COMMON for both mode of attendance and year of study - only consider current academic year
			is("studentCourseYearDetails.academicYear", AcademicYear.guessByDate(DateTime.now),
				"mostSignificantCourse" -> "studentCourseDetails",
				"studentCourseDetails.studentCourseYearDetails" -> "studentCourseYearDetails"
			),
			
			// SPR status
			inIfNotEmpty(
				"studentCourseDetails.sprStatus", sprStatuses.asScala, 
				"mostSignificantCourse" -> "studentCourseDetails"
			),
			
			// Registered modules
			inIfNotEmpty(
				"moduleRegistration.module", modules.asScala, 
				"mostSignificantCourse" -> "studentCourseDetails", 
				"studentCourseDetails.moduleRegistrations" -> "moduleRegistration"
			)
		).flatten
		
	private def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted
		
	private def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted
	
	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	def allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(department)
	def allCourseTypes: Seq[CourseType] = CourseType.all
	def allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(department)
	def allYearsOfStudy: Seq[Int] = 1 to 8
	def allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(department)
	def allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(department)
}

trait FilterStudentsState {
	def department: Department
	
	var studentsPerPage = DefaultStudentsPerPage
	var page = 1
	
	val order = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	
	var courseTypes: JSet[CourseType] = JHashSet()
	var routes: JSet[Route] = JHashSet()
	var modesOfAttendance: JSet[ModeOfAttendance] = JHashSet()
	var yearsOfStudy: JSet[JInteger] = JHashSet()
	var sprStatuses: JSet[SitsStatus] = JHashSet()
	var modules: JSet[Module] = JHashSet()
}

trait FilterStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FilterStudentsState =>
	
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Search, department)
	}
}