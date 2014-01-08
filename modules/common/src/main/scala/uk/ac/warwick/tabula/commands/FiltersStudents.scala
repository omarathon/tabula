package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.{Module, SitsStatus, ModeOfAttendance, Route, CourseType, Department}
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.util.web.UriBuilder
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods

object FiltersStudents {
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

trait FiltersStudents extends ProfileServiceComponent with PermissionsCheckingMethods {
	import FiltersStudents._

	def department: Department
	def courseTypes: JList[CourseType]
	def routes: JList[Route]
	def modesOfAttendance: JList[ModeOfAttendance]
	def yearsOfStudy: JList[JInteger]
	def sprStatuses: JList[SitsStatus]
	def modules: JList[Module]
	def defaultOrder: Seq[Order]
	def sortOrder: JList[Order]

	protected def buildRestrictions(): Seq[ScalaRestriction] = {
		Seq(
			// Course type
			startsWithIfNotEmpty(
				"course.code", courseTypes.asScala.map { _.courseCodeChar.toString },
				AliasPaths("course") : _*
			),

			// Route
			inIfNotEmpty(
				"studentCourseDetails.route.code", routes.asScala.map {_.code},
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
	}

	protected def buildOrders(): Seq[ScalaOrder] =
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
	lazy val allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(mandatory(department))
	lazy val allCourseTypes: Seq[CourseType] = mandatory(department).filterRule.courseTypes
	lazy val allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(mandatory(department)).sorted(Route.DegreeTypeOrdering)
	lazy val allYearsOfStudy: Seq[Int] = 1 to 8
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(mandatory(department).rootDepartment)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(mandatory(department))

	def serializeFilter = {
		val result = new UriBuilder()
		courseTypes.asScala.foreach(p => result.addQueryParameter("courseTypes", p.code))
		routes.asScala.foreach(p => result.addQueryParameter("routes", p.code))
		modesOfAttendance.asScala.foreach(p => result.addQueryParameter("modesOfAttendance", p.code))
		yearsOfStudy.asScala.foreach(p => result.addQueryParameter("yearsOfStudy", p.toString))
		sprStatuses.asScala.foreach(p => result.addQueryParameter("sprStatuses", p.code))
		modules.asScala.foreach(p => result.addQueryParameter("modules", p.code))
		if (result.getQuery == null)
			""
		else
			result.getQuery
	}
	def filterMap = {
		Map(
			"courseTypes" -> courseTypes.asScala.map{_.code}.mkString(","),
			"routes" -> routes.asScala.map{_.code}.mkString(","),
			"modesOfAttendance" -> modesOfAttendance.asScala.map{_.code}.mkString(","),
			"yearsOfStudy" -> yearsOfStudy.asScala.mkString(","),
			"sprStatuses" -> sprStatuses.asScala.map{_.code}.mkString(","),
			"modules" -> modules.asScala.map{_.code}.mkString(",")
		)
	}
}
