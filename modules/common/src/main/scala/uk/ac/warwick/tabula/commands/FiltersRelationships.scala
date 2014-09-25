package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model._

import scala.collection.JavaConverters._

object FiltersRelationships {
	val AliasPaths = Seq(
		"studentCourseYearDetails" -> Seq(
			"latestStudentCourseYearDetails" -> "studentCourseYearDetails"
		),
		"moduleRegistration" -> Seq(
			"_moduleRegistrations" -> "moduleRegistration"
		),
		"course" -> Seq(
			"course" -> "course"
		),
		"route" -> Seq(
			"route" -> "route"
		),
		"statusOnRoute" -> Seq(
			"statusOnRoute" -> "statusOnRoute"
		),
		"department" -> Seq(
			"route" -> "route",
			"route.department" -> "department"
		)
	).toMap

	val MaxStudentsPerPage = FilterStudentsOrRelationships.MaxStudentsPerPage
	val DefaultStudentsPerPage = FilterStudentsOrRelationships.DefaultStudentsPerPage
}
trait FiltersRelationships extends FilterStudentsOrRelationships {
	import uk.ac.warwick.tabula.commands.FiltersRelationships._

	def routeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"route.code", routes.asScala.map {_.code},
		getAliasPaths("route") : _*
	)

	def sprStatusRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"statusOnRoute", sprStatuses.asScala,
		getAliasPaths("statusOnRoute") : _*
	)

	def allDepartments: Seq[Department]
	def allRoutes: Seq[Route]

	override def getAliasPaths(sitsTable: String) = AliasPaths(sitsTable)

	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = allDepartments.map(modulesForDepartmentAndSubDepartments).flatten
	lazy val allCourseTypes: Seq[CourseType] = CourseType.all
	lazy val allSprStatuses: Seq[SitsStatus] = allDepartments.map(dept => profileService.allSprStatuses(dept.rootDepartment)).flatten.distinct
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = allDepartments.map(profileService.allModesOfAttendance).flatten.distinct

}
