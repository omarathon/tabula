package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods

object FiltersRelationships {
	val MaxStudentsPerPage = 100
	val DefaultStudentsPerPage = 50

	val AliasPaths = Seq(
		"studentCourseYearDetails" -> Seq(
			"latestStudentCourseYearDetails" -> "studentCourseYearDetails"
		),
		"moduleRegistration" -> Seq(
			"moduleRegistrations" -> "moduleRegistration"
		),
		"course" -> Seq(
			"course" -> "course"
		),
		"route" -> Seq(
			"route" -> "route"
		),
		"sprStatus" -> Seq(
			"sprStatus" -> "sprStatus"
		)
	).toMap
}

trait FiltersRelationships extends FiltersStudentsBase with ProfileServiceComponent with PermissionsCheckingMethods {
	import FiltersRelationships._

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
				"route.code", routes.asScala.map {_.code},
				AliasPaths("route") : _*
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
				"sprStatus", sprStatuses.asScala,
				AliasPaths("sprStatus") : _*
			),

			// Registered modules
			inIfNotEmpty(
				"moduleRegistration.module", modules.asScala,
				AliasPaths("moduleRegistration") : _*
			)
		).flatten
	}

	var allDepartments : Seq[Department] = _
	var allRoutes : Seq[Route] = _

	// Do we need to consider out-of-department modules/routes or can we rely on users typing them in manually?
	lazy val allModules: Seq[Module] = allDepartments.map(modulesForDepartmentAndSubDepartments(_)).flatten
	lazy val allCourseTypes: Seq[CourseType] = CourseType.all
	lazy val allYearsOfStudy: Seq[Int] = 1 to 8
	lazy val allSprStatuses: Seq[SitsStatus] = allDepartments.map(dept => profileService.allSprStatuses(dept.rootDepartment)).flatten.distinct
	lazy val allModesOfAttendance: Seq[ModeOfAttendance] = allDepartments.map(profileService.allModesOfAttendance(_)).flatten.distinct


}
