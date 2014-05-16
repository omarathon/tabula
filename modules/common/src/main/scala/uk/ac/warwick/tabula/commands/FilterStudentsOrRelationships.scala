package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.{SitsStatusDaoComponent, ModeOfAttendanceDaoComponent, ScalaRestriction, ScalaOrder}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, CourseAndRouteServiceComponent, ProfileServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.data.ScalaRestriction._

object FilterStudentsOrRelationships {

	val MaxStudentsPerPage = 100
	val DefaultStudentsPerPage = 50

}

trait FilterStudentsOrRelationships extends FiltersStudentsBase with PermissionsCheckingMethods
 with ProfileServiceComponent with CourseAndRouteServiceComponent with ModeOfAttendanceDaoComponent
	with SitsStatusDaoComponent with ModuleAndDepartmentServiceComponent {

	def getAliasPaths(sitsTable: String): Seq[(String, String)]

	protected def buildOrders(): Seq[ScalaOrder] =
		(sortOrder.asScala ++ defaultOrder).map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying, getAliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying)
			}
		}


	def courseRestriction: Option[ScalaRestriction] = startsWithIfNotEmpty(
		"course.code", courseTypes.asScala.map { _.courseCodeChar.toString },
		getAliasPaths("course") : _*
	)

	def attendanceRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseYearDetails.modeOfAttendance", modesOfAttendance.asScala,
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def courseTypeRestriction: Option[ScalaRestriction] = startsWithIfNotEmpty(
		"course.code", courseTypes.asScala.map { _.courseCodeChar.toString },
		getAliasPaths("course") : _*
	)

	def routeRestriction: Option[ScalaRestriction]

	def yearOfStudyRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseYearDetails.yearOfStudy", yearsOfStudy.asScala,
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def registeredModulesRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"moduleRegistration.module", modules.asScala,
		getAliasPaths("moduleRegistration") : _*
	)

	def sprStatusRestriction: Option[ScalaRestriction]

	def tier4Restriction: Option[ScalaRestriction] = atLeastOneIsTrue(
		"studentCourseYearDetails.casUsed", "studentCourseYearDetails.tier4Visa", otherCriteria.contains("Tier 4 only"),
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def visitingRestriction: Option[ScalaRestriction] = isIfTicked(
		"department.code",
		"io",
		otherCriteria.contains("Visiting"),
		getAliasPaths("department") : _*
	)

	protected def buildRestrictions(): Seq[ScalaRestriction] = {
		Seq(
				courseTypeRestriction,
				routeRestriction,
				attendanceRestriction,
				yearOfStudyRestriction,
				sprStatusRestriction,
				registeredModulesRestriction,
				tier4Restriction,
				visitingRestriction
		).flatten
	}

	lazy val allYearsOfStudy: Seq[Int] = 1 to 8
	lazy val allOtherCriteria: Seq[String] = Seq("Tier 4 only", "Visiting")
}
