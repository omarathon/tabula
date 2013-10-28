package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.helpers.StringUtils._
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
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.ScalaOrder

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
	
	val AliasPaths = Seq(
		"studentCourseDetails" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails"
		),
		"studentCourseYearDetails" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.latestStudentCourseYearDetails" -> "studentCourseYearDetails"
		),
		"moduleRegistration" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.moduleRegistrations" -> "moduleRegistration"
		),
		"course" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.course" -> "course"
		),
		"route" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.route" -> "route"
		)
	).toMap
}

class FilterStudentsCommand(val department: Department) extends CommandInternal[FilterStudentsResults] with FilterStudentsState with BindListener {
	self: ProfileServiceComponent =>
	
	def applyInternal() = {
		val totalResults = profileService.countStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions()
		)
		
		val students = profileService.findStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = buildOrders(), 
			maxResults = studentsPerPage, 
			startResult = (studentsPerPage * (page-1))
		)
		
		FilterStudentsResults(students, totalResults)
	}
	
	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (sprStatuses.isEmpty()) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
	}
	
	private def buildRestrictions(): Seq[ScalaRestriction] = 
		Seq(
			// Course type
			startsWithIfNotEmpty(
				"course.code", courseTypes.asScala.map { _.courseCodeChar.toString }, 
				AliasPaths("course") : _*
			),
				
			// Route
			inIfNotEmpty(
				"studentCourseDetails.route", routes.asScala, 
				AliasPaths("studentCourseDetails") : _*
			),
			
			// Mode of attendance
			inIfNotEmpty(
				"studentCourseYearDetails.modeOfAttendance", modesOfAttendance.asScala, 
				AliasPaths("studentCourseYearDetails") : _*
			),
			
			// Year of study
			inIfNotEmpty(
				"studentCourseYearDetails.yearOfStudy", yearsOfStudy.asScala, 
				AliasPaths("studentCourseYearDetails") : _*
			),
			
			// SPR status
			inIfNotEmpty(
				"studentCourseDetails.sprStatus", sprStatuses.asScala, 
				AliasPaths("studentCourseDetails") : _*
			),
			
			// Registered modules
			inIfNotEmpty(
				"moduleRegistration.module", modules.asScala, 
				AliasPaths("moduleRegistration") : _*
			)
		).flatten
		
	private def buildOrders(): Seq[ScalaOrder] =
		(sortOrder.asScala ++ defaultOrder).map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying, AliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying)
			}
		}
		
	private def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted
		
	private def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted
	
	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(department)
	lazy val allCourseTypes: Seq[CourseType] = CourseType.all
	lazy val allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(department)
	lazy val allYearsOfStudy: Seq[Int] = 1 to 8
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(department)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(department)
}

trait FilterStudentsState {
	def department: Department
	
	var studentsPerPage = DefaultStudentsPerPage
	var page = 1
	
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList() 
	
	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}

trait FilterStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FilterStudentsState =>
	
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Search, department)
	}
}