package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.{Module, SitsStatus, ModeOfAttendance, Route, CourseType, Department}
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.services.ProfileServiceComponent

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
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

trait FiltersStudents extends FiltersStudentsBase with ProfileServiceComponent with PermissionsCheckingMethods {
	import FiltersStudents._

	def department: Department

	protected def buildOrders(): Seq[ScalaOrder] =
		(sortOrder.asScala ++ defaultOrder).map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying, AliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying)
			}
		}

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
				"studentCourseDetails.statusOnRoute", sprStatuses.asScala,
				AliasPaths("studentCourseDetails") : _*
			),

			// Registered modules
			inIfNotEmpty(
				"moduleRegistration.module", modules.asScala,
				AliasPaths("moduleRegistration") : _*
			)
		).flatten
	}


	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = modulesForDepartmentAndSubDepartments(mandatory(department))
	lazy val allCourseTypes: Seq[CourseType] = mandatory(department).filterRule.courseTypes
	lazy val allRoutes: Seq[Route] = routesForDepartmentAndSubDepartments(mandatory(department)).sorted(Route.DegreeTypeOrdering)
	lazy val allYearsOfStudy: Seq[Int] = 1 to 8
	lazy val allSprStatuses: Seq[SitsStatus] = profileService.allSprStatuses(mandatory(department).rootDepartment)
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = profileService.allModesOfAttendance(mandatory(department))


}
