package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.{Module, SitsStatus, ModeOfAttendance, Route, CourseType, Department}
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.ScalaRestriction._

import scala.collection.JavaConverters._

object FiltersStudents {
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
			"studentCourseDetails._moduleRegistrations" -> "moduleRegistration"
		),
		"course" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.course" -> "course"
		),
		"route" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.route" -> "route"
		),
		"department" -> Seq(
			"mostSignificantCourse" -> "studentCourseDetails",
			"studentCourseDetails.route" -> "route",
			"route.department" -> "department"
		)
	).toMap

	val MaxStudentsPerPage = FilterStudentsOrRelationships.MaxStudentsPerPage
	val DefaultStudentsPerPage = FilterStudentsOrRelationships.DefaultStudentsPerPage
}

trait FiltersStudents extends FilterStudentsOrRelationships {
	import FiltersStudents._

	def department: Department

	def routeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseDetails.route.code", routes.asScala.map {_.code},
		getAliasPaths("studentCourseDetails") : _*
	)

	def sprStatusRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseDetails.statusOnRoute", sprStatuses.asScala,
		getAliasPaths("studentCourseDetails") : _*
	)

	override def getAliasPaths(sitsTable: String) = AliasPaths(sitsTable)

	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(mandatory(department))
	lazy val allCourseTypes: Seq[CourseType] = mandatory(department).filterRule.courseTypes
	lazy val allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment)).sorted(Route.DegreeTypeOrdering)
		case routes => routes.sorted(Route.DegreeTypeOrdering)
	}
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(mandatory(department).rootDepartment)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(mandatory(department).rootDepartment)
}
