package uk.ac.warwick.tabula.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.{ScalaRestriction, ScalaOrder}
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.data.ScalaRestriction._

object FilterStudentsOrRelationships {

	val MaxStudentsPerPage = 100
	val DefaultStudentsPerPage = 50
	val MaxYearsOfStudy = 12

}

trait FilterStudentsOrRelationships extends FiltersStudentsBase with PermissionsCheckingMethods with ProfileServiceComponent {

	def getAliasPaths(sitsTable: String): Seq[(String, String)]

	protected def buildOrders(): Seq[ScalaOrder] =
		(sortOrder.asScala ++ defaultOrder).map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying, getAliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying)
			}
		}

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

	//Seq(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
	def registeredModulesRestriction(year: AcademicYear): Option[ScalaRestriction] = inIfNotEmptyMultipleProperties(
		Seq("moduleRegistration.module", "moduleRegistration.academicYear"),
		Seq(modules.asScala, Seq(year)),
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

	protected def buildRestrictions(year: AcademicYear): Seq[ScalaRestriction] = {
		Seq(
				courseTypeRestriction,
				routeRestriction,
				attendanceRestriction,
				yearOfStudyRestriction,
				sprStatusRestriction,
				registeredModulesRestriction(year),
				tier4Restriction,
				visitingRestriction
		).flatten
	}

	lazy val allYearsOfStudy: Seq[Int] = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
	lazy val allOtherCriteria: Seq[String] = Seq("Tier 4 only", "Visiting")
}
