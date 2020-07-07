package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import org.joda.time.{DateTime, DateTimeConstants, LocalDate, LocalTime}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

sealed abstract class MarkState(val description: String, val cssClass: String) extends EnumEntry
object MarkState extends Enum[MarkState] {
  override val values: IndexedSeq[MarkState] = findValues

  case object UnconfirmedActual extends MarkState("Unconfirmed actual", "default")
  case object ConfirmedActual extends MarkState("Confirmed actual", "info") // could be named Actual but being explicit also helps with Actual/Agreed confusion
  case object Agreed extends MarkState("Agreed", "success")

  val DecisionReleaseTime = new LocalTime(10, 0)
  val MarkUploadTime = new LocalTime(9, 40) // Give us 20 minutes to upload agreed marks before release

  // https://warwick.ac.uk/coronavirus/intranet/continuity/teaching/marksandexamboards/guidance/results/#coordinatedrelease
  val DegreeApprenticeshipFirstYearReleaseDate2020 = new LocalDate(2020, DateTimeConstants.JULY, 29)
  val UndergraduateFirstYearReleaseDate2020 = new LocalDate(2020, DateTimeConstants.JULY, 9)
  val UndergraduateFinalistReleaseDate2020 = new LocalDate(2020, DateTimeConstants.JULY, 22)
  val UndergraduateIntermediateReleaseDate2020 = new LocalDate(2020, DateTimeConstants.JULY, 30)
  val PostgraduateTaughtReleaseDate2020 = new LocalDate(2020, DateTimeConstants.JULY, 8)

  def resultsReleasedToStudents(moduleRegistration: ModuleRegistration, releaseTime: LocalTime): Boolean =
    resultsReleasedToStudents(moduleRegistration.academicYear, Option(moduleRegistration.studentCourseDetails), releaseTime)

  def resultsReleasedToStudents(academicYear: AcademicYear, studentCourseDetails: Option[StudentCourseDetails], releaseTime: LocalTime): Boolean = {
    // Include previous years as well for (e.g.) resits, that'll work for 19/20 at least.
    if (academicYear <= AcademicYear.starting(2019)) {
      val releaseDate: Option[LocalDate] = studentCourseDetails.collect {
        case scd if scd.course.code.startsWith("D") && scd.latestStudentCourseYearDetails.yearOfStudy == 1 =>
          DegreeApprenticeshipFirstYearReleaseDate2020

        case scd if scd.courseType.contains(CourseType.UG) && scd.latestStudentCourseYearDetails.yearOfStudy == 1 =>
          UndergraduateFirstYearReleaseDate2020

        case scd if scd.courseType.contains(CourseType.UG) && scd.latestStudentCourseYearDetails.isFinalYear =>
          UndergraduateFinalistReleaseDate2020

        case scd if scd.courseType.contains(CourseType.UG) =>
          UndergraduateIntermediateReleaseDate2020

        case scd if scd.courseType.contains(CourseType.PGT) =>
          PostgraduateTaughtReleaseDate2020
      }

      // Fail open, if the student doesn't match any rules, allow pushing
      releaseDate.forall(_.toDateTime(releaseTime).isBefore(DateTime.now()))
    } else true
  }
}

class MarkStateUserType extends EnumUserType(MarkState)
class MarkStateConverter extends EnumTwoWayConverter(MarkState)
