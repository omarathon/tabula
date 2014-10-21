package uk.ac.warwick.tabula.commands

import org.hibernate.sql.JoinType
import uk.ac.warwick.tabula.data.model.{Module, SitsStatus, ModeOfAttendance, Route, CourseType, Department}
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._

import scala.collection.JavaConverters._

object FiltersStudents {
	val AliasPaths: Map[String, Seq[(String, AliasAndJoinType)]] = Seq(
		"studentCourseDetails" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails")
		),
		"studentCourseYearDetails" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails.latestStudentCourseYearDetails" -> AliasAndJoinType("studentCourseYearDetails")
		),
		"moduleRegistration" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails._moduleRegistrations" -> AliasAndJoinType("moduleRegistration")
		),
		"course" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails.course" -> AliasAndJoinType("course")
		),
		"route" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails.route" -> AliasAndJoinType("route")
		),
		"department" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails.route" -> AliasAndJoinType("route"),
			"route.adminDepartment" -> AliasAndJoinType("department")
		),
		"teachingInfo" -> Seq(
			"mostSignificantCourse" -> AliasAndJoinType("studentCourseDetails"),
			"studentCourseDetails.route" -> AliasAndJoinType("route"),
			"route.teachingInfo" -> AliasAndJoinType("teachingInfo", JoinType.LEFT_OUTER_JOIN)
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

	lazy val allModules: Seq[Module] = ((modulesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => modulesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case modules => modules
	}) ++ modules.asScala).distinct.sorted
	lazy val allRoutes: Seq[Route] = ((routesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case routes => routes
	}) ++ routes.asScala).distinct.sorted(Route.DegreeTypeOrdering)

	lazy val allCourseTypes: Seq[CourseType] = mandatory(department).filterRule.courseTypes
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(mandatory(department).rootDepartment)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(mandatory(department).rootDepartment)
}