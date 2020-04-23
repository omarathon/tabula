package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.{DateTime, DateTimeConstants}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AssessmentService, AssessmentServiceComponent, CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

class EditAssignmentDetailsValidationTest extends TestBase with Mockito {

  trait Fixture {
    val validator = new EditAssignmentDetailsValidation with EditAssignmentDetailsCommandState with EditAssignmentDetailsRequest
    with UserLookupComponent with AssessmentServiceComponent with CM2MarkingWorkflowServiceComponent {
      val assignment: Assignment = Fixtures.assignment("this")
      val assessmentService: AssessmentService = smartMock[AssessmentService]
      val userLookup = new MockUserLookup
      val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
    }

    val openDateNewYearsDay = new DateTime(2020, DateTimeConstants.JANUARY, 1, 0, 0, 0, 0)
    val validOpenDate = new DateTime(2020, DateTimeConstants.JANUARY, 8, 0, 0, 0, 0)
    val validCloseDateWithValidTime: DateTime = validOpenDate.plusDays(7).withHourOfDay(12)
    val validCloseDateWithOutOfHoursTime: DateTime = validOpenDate.plusDays(7).withHourOfDay(7)

  }

  @Test def validateOpenDateCannotBeAfterCloseDate(): Unit = new Fixture {
    validator.openDate = validOpenDate.toLocalDate
    validator.closeDate = validOpenDate.minusDays(1).withHourOfDay(12)
    validator.createdByAEP = false
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("closeDate")
    errors.getFieldError.getCode should be("closeDate.early")
  }

  @Test def validateOpenDateCannotBeAfterCloseDateEvenWithAEP(): Unit = new Fixture {
    validator.openDate = validOpenDate.toLocalDate
    validator.closeDate = validOpenDate.minusDays(1).withHourOfDay(12)
    validator.createdByAEP = true
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("closeDate")
    errors.getFieldError.getCode should be("closeDate.early")
  }

  @Test def validateOpenDateCannotBeAHoliday(): Unit = new Fixture {
    validator.openDate = openDateNewYearsDay.toLocalDate
    validator.closeDate = validCloseDateWithValidTime
    validator.createdByAEP = false
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("openDate")
    errors.getFieldError.getCode should be("openDate.notWorkingDay")
  }

  @Test def validateOpenDateCanBeAHolidayForAEP(): Unit = new Fixture {
    validator.openDate = openDateNewYearsDay.toLocalDate
    validator.closeDate = validCloseDateWithValidTime
    validator.createdByAEP = true
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(false)
  }

  @Test def validateCloseDateAllowedTimes(): Unit = new Fixture {
    validator.openDate = validOpenDate.toLocalDate
    validator.closeDate = validCloseDateWithOutOfHoursTime
    validator.createdByAEP = false
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("closeDate")
    errors.getFieldError.getCode should be("closeDate.invalidTime")
  }

  @Test def validateCloseDateCanBeOutsideAllowedTimesForAEP(): Unit = new Fixture {
    validator.openDate = validOpenDate.toLocalDate
    validator.closeDate = validCloseDateWithOutOfHoursTime
    validator.createdByAEP = true
    val errors = new BindException(validator, "command")
    validator.validate(errors)
    errors.hasErrors should be(false)
  }
}
