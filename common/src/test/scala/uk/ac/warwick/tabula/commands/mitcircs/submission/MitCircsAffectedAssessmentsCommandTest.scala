package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.{AcademicYear, TestBase}

class MitCircsAffectedAssessmentsCommandTest extends TestBase {

  private def validatingRequest(startDate: LocalDate, endDate: Option[LocalDate]): MitCircsAffectedAssessmentsRequest with MitCircsAffectedAssessmentsValidation = {
    val request = new MitCircsAffectedAssessmentsRequest with MitCircsAffectedAssessmentsValidation {}
    request.startDate = startDate
    endDate.foreach(request.endDate = _)
    request
  }

  @Test def validates(): Unit = {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.JANUARY, 3),
      endDate = Some(new LocalDate(2016, DateTimeConstants.FEBRUARY, 1))
    )

    val errors = new BindException(request, "request")
    request.validate(errors)

    errors.hasErrors should be(false)
  }

  @Test def validatesMissingStartDate(): Unit = {
    val request = validatingRequest(
      startDate = null,
      endDate = Some(new LocalDate(2016, DateTimeConstants.FEBRUARY, 1))
    )

    val errors = new BindException(request, "request")
    request.validate(errors)

    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("startDate")
    errors.getFieldError.getCode should be("mitigatingCircumstances.startDate.required")
  }

  @Test def validatesOngoing(): Unit = withFakeTime(new DateTime(2016, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)) {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.JANUARY, 3),
      endDate = None
    )

    val errors = new BindException(request, "request")
    request.validate(errors)

    errors.hasErrors should be(false)
  }

  @Test def validatesEndBeforeStart(): Unit = {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.FEBRUARY, 1),
      endDate = Some(new LocalDate(2016, DateTimeConstants.JANUARY, 3))
    )

    val errors = new BindException(request, "request")
    request.validate(errors)

    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("endDate")
    errors.getFieldError.getCode should be("mitigatingCircumstances.endDate.after")
  }

  @Test def requestYears(): Unit = {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.FEBRUARY, 1),
      endDate = Some(new LocalDate(2016, DateTimeConstants.JANUARY, 3))
    )

    request.academicYears should be (Seq(AcademicYear.starting(2015)))
  }

  @Test def requestYearsOngoing(): Unit = withFakeTime(new DateTime(2016, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)) {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.FEBRUARY, 1),
      endDate = None
    )

    request.academicYears should be (Seq(AcademicYear.starting(2015)))
  }

  @Test def requestYearsRange(): Unit = {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.FEBRUARY, 1),
      endDate = Some(new LocalDate(2018, DateTimeConstants.JANUARY, 3))
    )

    request.academicYears should be (Seq(AcademicYear.starting(2015), AcademicYear.starting(2016), AcademicYear.starting(2017)))
  }

  @Test def requestYearsRangeOngoing(): Unit = withFakeTime(new DateTime(2016, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)) {
    val request = validatingRequest(
      startDate = new LocalDate(2016, DateTimeConstants.FEBRUARY, 1),
      endDate = None
    )

    request.academicYears should be (Seq(AcademicYear.starting(2015)))
  }

  @Test def requestYearsFutureOngoing(): Unit = withFakeTime(new DateTime(2016, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)) {
    val request = validatingRequest(
      startDate = new LocalDate(2017, DateTimeConstants.FEBRUARY, 1),
      endDate = None
    )

    request.academicYears should be (Seq(AcademicYear.starting(2016)))
  }

}
