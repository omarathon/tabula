package uk.ac.warwick.tabula.helpers.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert.JodaDateTimeConverter
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{InvestigationCompleted, NotInvestigated, SuspectPlagiarised}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{Extension, SavedFormValue, WordCountField}
import uk.ac.warwick.tabula.helpers.cm2._
import uk.ac.warwick.tabula.services.{ExtensionService, FeedbackService}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters._

// scalastyle:off magic.number
class CourseworkFiltersTest extends TestBase with Mockito {

  val department: Department = Fixtures.department("in", "IT Services")
  val module: Module = Fixtures.module("in101", "Introduction to Web Development")
  val assignment: Assignment = Fixtures.assignment("Programming Test")
  assignment.module = module
  module.adminDepartment = department

  assignment.feedbackService = smartMock[FeedbackService]
  assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

  val mockUserLookup = new MockUserLookup
  mockUserLookup.registerUserObjects(
    new User("cuscav") {
      setWarwickId("0672089"); setFoundUser(true); setVerified(true)
    }
  )
  assignment.userLookup = mockUserLookup

  assignment.extensionService = smartMock[ExtensionService]
  assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty


  @Test(expected = classOf[IllegalArgumentException]) def invalidFilter(): Unit = {
    CourseworkFilters.of("Spank")
  }

  @Test def name(): Unit = {
    CourseworkFilters.AllStudents.getName should be("AllStudents")
    CourseworkFilters.of("NotReleasedForMarking").getName should be("NotReleasedForMarking")
  }

  @Test def AllStudents(): Unit = {
    val filter = CourseworkFilters.AllStudents

    // Should pass anything and any assignment, so just check with null
    filter.applies(null) should be (true)
    filter.predicate(null) should be (true)
  }

  private def workflowItems(
    submission: Option[Submission] = None,
    submissionDownloaded: Boolean = false,
    feedback: Option[Feedback] = None,
    feedbackDownloaded: Boolean = false,
    onlineFeedbackViewed: Boolean = false,
    extension: Option[Extension] = None,
    withinExtension: Boolean = false,
    student: User = null) =
    WorkflowItems(
      student = student,
      enhancedSubmission = submission map { s => SubmissionListItem(s, submissionDownloaded) },
      enhancedFeedback = feedback map { f => FeedbackListItem(f, feedbackDownloaded, onlineFeedbackViewed, Some(new FeedbackForSits)) },
      enhancedExtension = extension map { e => ExtensionListItem(e, withinExtension) }
    )

  private def student(
    submission: Option[Submission] = None,
    submissionDownloaded: Boolean = false,
    feedback: Option[Feedback] = None,
    feedbackDownloaded: Boolean = false,
    onlineFeedbackViewed: Boolean = false,
    extension: Option[Extension] = None,
    withinExtension: Boolean = false,
    assignment: Assignment = null,
    user: User = Fixtures.user()) =
    AssignmentSubmissionStudentInfo(
      user = user,
      progress = null,
      nextStage = None,
      stages = ListMap.empty,
      coursework = workflowItems(submission, submissionDownloaded, feedback, feedbackDownloaded, onlineFeedbackViewed, extension, withinExtension),
      assignment = assignment,
      disability = None,
      reasonableAdjustmentsDeclared = None,
    )

  class SampleFilteringCommand(elems: (String, String)*) {
    var filter: JMap[String, String] = JHashMap()
    elems foreach { case (k, v) => filter.put(k, v) }
  }

  @Test def SubmittedBetweenDates(): Unit = {
    val filter = CourseworkFilters.SubmittedBetweenDates

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    val converter = new JodaDateTimeConverter

    // Validation - start and end date must be defined and end must be after start
    {
      val cmd = new SampleFilteringCommand()
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(2)
      errors.getFieldErrors.asScala.head.getField should be("filter[startDate]")
      errors.getFieldErrors.asScala.head.getCodes should contain("NotEmpty")
      errors.getFieldErrors.asScala(1).getField should be("filter[endDate]")
      errors.getFieldErrors.asScala(1).getCodes should contain("NotEmpty")
    }

    {
      val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
      val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

      val cmd = new SampleFilteringCommand(
        "startDate" -> converter.convertLeft(endDate),
        "endDate" -> converter.convertLeft(startDate)
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(1)
      errors.getFieldErrors.asScala.head.getField should be("filter[endDate]")
      errors.getFieldErrors.asScala.head.getCode should be("filters.SubmittedBetweenDates.end.beforeStart")
    }

    {
      val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
      val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

      val cmd = new SampleFilteringCommand(
        "startDate" -> converter.convertLeft(startDate),
        "endDate" -> converter.convertLeft(endDate)
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.hasErrors should be (false)
    }

    // Valid where there is a submission, and the submission is in March
    val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
    val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

    val params = Map(
      "startDate" -> converter.convertLeft(startDate),
      "endDate" -> converter.convertLeft(endDate)
    )

    filter.predicate(params)(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    // Submission in February
    submission.submittedDate = startDate.minusDays(1)
    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    // Submission in April
    submission.submittedDate = endDate.plusDays(1)
    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    // Submission exactly on start date
    submission.submittedDate = startDate
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    // Submission exactly on end date
    submission.submittedDate = endDate
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    // Submission inbetween
    submission.submittedDate = startDate.plusDays(1)
    filter.predicate(params)(student(submission = Option(submission))) should be (true)
  }

  @Test def OnTime(): Unit = {
    val filter = CourseworkFilters.OnTime

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid where there is a submission, that submission is not late, and that submission is not authorised late
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (false)

    filter.predicate(student(submission = Option(submission))) should be (true)

    // Where submission is late, they don't fit
    assignment.closeDate = DateTime.now.minusDays(1)
    submission.submittedDate = DateTime.now

    submission.isLate should be (true)
    submission.isAuthorisedLate should be (false)
    filter.predicate(student(submission = Option(submission))) should be (false)

    // Authorised late isn't allowed here either
    // TODO is this right?

    val extension = Fixtures.extension("0672089", "cuscav")
    extension.approve()
    extension.expiryDate = DateTime.now.plusDays(1)
    assignment.addExtension(extension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (true)
    filter.predicate(student(submission = Option(submission), extension = Option(extension), withinExtension = true)) should be (false)
  }

  @Test def Late(): Unit = {
    val filter = CourseworkFilters.Late

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid where there is a submission, that submission is late, and that submission is not authorised late
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (false)

    filter.predicate(student(submission = Option(submission))) should be (false)

    // Where submission is late, they don't fit
    assignment.closeDate = DateTime.now.minusDays(1)
    submission.submittedDate = DateTime.now

    submission.isLate should be (true)
    submission.isAuthorisedLate should be (false)
    filter.predicate(student(submission = Option(submission))) should be (true)

    // Authorised late isn't allowed here

    val extension = Fixtures.extension("0672089", "cuscav")
    extension.approve()
    extension.expiryDate = DateTime.now.plusDays(1)
    assignment.addExtension(extension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (true)
    filter.predicate(student(submission = Option(submission), extension = Option(extension), withinExtension = true)) should be (false)
  }

  @Test def WithExtension(): Unit = {
    val filter = CourseworkFilters.WithExtension

    // Only applies to assignments that collect submissions and accept extensions
    assignment.allowExtensions = false

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    assignment.allowExtensions = true

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when there is an extension
    filter.predicate(student(extension = None)) should be (false)
    filter.predicate(student(extension = Option(Fixtures.extension()))) should be (true)
  }

  @Test def WithinExtension(): Unit = {
    val filter = CourseworkFilters.WithinExtension

    // Only applies to assignments that collect submissions and accept extensions
    assignment.allowExtensions = false

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    assignment.allowExtensions = true

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid only when the submission is authorised late (NOT when we are just within extension - else we wouldn't have submitted)
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (false)

    filter.predicate(student(submission = Option(submission))) should be (false)

    // Where submission is late, they don't fit
    assignment.closeDate = DateTime.now.minusDays(1)
    submission.submittedDate = DateTime.now

    submission.isLate should be (true)
    submission.isAuthorisedLate should be (false)
    filter.predicate(student(submission = Option(submission))) should be (false)

    // Authorised late fits

    val extension = Fixtures.extension("0672089", "cuscav")
    extension.approve()
    extension.expiryDate = DateTime.now.plusDays(1)
    assignment.addExtension(extension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(extension.usercode -> extension)

    submission.isLate should be (false)
    submission.isAuthorisedLate should be (true)
    filter.predicate(student(submission = Option(submission), extension = Option(extension), withinExtension = true)) should be (true)
  }

  @Test def WithWordCount(): Unit = {
    val filter = CourseworkFilters.WithWordCount

    // Only applies to assignments that collect submissions and have a word count field defined
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    val wordCountField = new WordCountField
    wordCountField.name = Assignment.defaultWordCountName
    assignment.addField(wordCountField)

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Validation - min and max must be defined, in range, and max must be >= min
    {
      val cmd = new SampleFilteringCommand()
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(2)
      errors.getFieldErrors.asScala.head.getField should be("filter[minWords]")
      errors.getFieldErrors.asScala.head.getCodes should contain("NotEmpty")
      errors.getFieldErrors.asScala(1).getField should be("filter[maxWords]")
      errors.getFieldErrors.asScala(1).getCodes should contain("NotEmpty")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minWords" -> 50.toString,
        "maxWords" -> "steve"
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(1)
      errors.getFieldErrors.asScala.head.getField should be("filter[maxWords]")
      errors.getFieldErrors.asScala.head.getCodes should contain("typeMismatch")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minWords" -> (-15).toString,
        "maxWords" -> (-200).toString
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(3)
      errors.getFieldErrors.asScala.head.getField should be("filter[maxWords]")
      errors.getFieldErrors.asScala.head.getCodes should contain("filters.WithWordCount.max.lessThanMin")
      errors.getFieldErrors.asScala(1).getField should be("filter[minWords]")
      errors.getFieldErrors.asScala(1).getCodes should contain("filters.WithWordCount.min.lessThanZero")
      errors.getFieldErrors.asScala(2).getField should be("filter[maxWords]")
      errors.getFieldErrors.asScala(2).getCodes should contain("filters.WithWordCount.max.lessThanZero")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minWords" -> "0",
        "maxWords" -> "100"
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.hasErrors should be (false)
    }

    // Valid where there is a submission, and the word count is between 40 and 60
    val params = Map(
      "minWords" -> "40",
      "maxWords" -> "60"
    )

    filter.predicate(params)(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    val v = new SavedFormValue
    v.name = wordCountField.name
    v.value = "30"
    submission.values.add(v)

    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    v.value = "40"
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    v.value = "50"
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    v.value = "60"
    filter.predicate(params)(student(submission = Option(submission))) should be (true)
  }

  @Test def Submitted(): Unit = {
    val filter = CourseworkFilters.Submitted

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when there is a submission
    filter.predicate(student(submission = None)) should be (false)
    filter.predicate(student(submission = Option(Fixtures.submission()))) should be (true)
  }

  @Test def Unsubmitted(): Unit = {
    val filter = CourseworkFilters.Unsubmitted

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when there is no submission
    filter.predicate(student(submission = None)) should be (true)
    filter.predicate(student(submission = Option(Fixtures.submission()))) should be (false)
  }

  @Test def CheckedForPlagiarism(): Unit = {
    val filter = CourseworkFilters.CheckedForPlagiarism

    // Only applies to assignments that collect submissions and the department has plagiarism detection enabled
    department.plagiarismDetectionEnabled = false

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    department.plagiarismDetectionEnabled = true

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when the submission exists and it has at least one attachment with an originality report
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.hasOriginalityReport.booleanValue() should be (false)

    filter.predicate(student(submission = Option(submission))) should be (false)

    val a = new FileAttachment
    val originalityReport = new OriginalityReport
    originalityReport.reportReceived = true
    a.originalityReport = originalityReport
    submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

    submission.hasOriginalityReport.booleanValue() should be (true)

    filter.predicate(student(submission = Option(submission))) should be (true)
  }

  @Test def WithOverlapPercentage(): Unit = {
    val filter = CourseworkFilters.WithOverlapPercentage

    // Only applies to assignments that collect submissions and the department has plagiarism detection enabled
    department.plagiarismDetectionEnabled = false

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    department.plagiarismDetectionEnabled = true

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Validation - min and max must be defined, in range, and max must be >= min
    {
      val cmd = new SampleFilteringCommand()
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(2)
      errors.getFieldErrors.asScala.head.getField should be("filter[minOverlap]")
      errors.getFieldErrors.asScala.head.getCodes should contain("NotEmpty")
      errors.getFieldErrors.asScala(1).getField should be("filter[maxOverlap]")
      errors.getFieldErrors.asScala(1).getCodes should contain("NotEmpty")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minOverlap" -> 50.toString,
        "maxOverlap" -> "steve"
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(1)
      errors.getFieldErrors.asScala.head.getField should be("filter[maxOverlap]")
      errors.getFieldErrors.asScala.head.getCodes should contain("typeMismatch")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minOverlap" -> 1500.toString,
        "maxOverlap" -> 200.toString
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.getErrorCount should be(3)
      errors.getFieldErrors.asScala.head.getField should be("filter[maxOverlap]")
      errors.getFieldErrors.asScala.head.getCodes should contain("filters.WithOverlapPercentage.max.lessThanMin")
      errors.getFieldErrors.asScala(1).getField should be("filter[minOverlap]")
      errors.getFieldErrors.asScala(1).getCodes should contain("filters.WithOverlapPercentage.min.notInRange")
      errors.getFieldErrors.asScala(2).getField should be("filter[maxOverlap]")
      errors.getFieldErrors.asScala(2).getCodes should contain("filters.WithOverlapPercentage.max.notInRange")
    }

    {
      val cmd = new SampleFilteringCommand(
        "minOverlap" -> "0",
        "maxOverlap" -> "100"
      )
      val errors = new BindException(cmd, "cmd")
      filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

      errors.hasErrors should be (false)
    }

    // Valid where there is a submission, and the overlap percentage is between 40 and 60
    val params = Map(
      "minOverlap" -> "40",
      "maxOverlap" -> "60"
    )

    filter.predicate(params)(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.hasOriginalityReport.booleanValue() should be (false)

    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    val a = new FileAttachment

    a.originalityReport = new OriginalityReport
    a.originalityReport.reportReceived = true
    a.originalityReport.overlap = Option(30)
    submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

    submission.hasOriginalityReport.booleanValue() should be (true)

    filter.predicate(params)(student(submission = Option(submission))) should be (false)

    a.originalityReport.overlap = Option(40)
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    a.originalityReport.overlap = Option(50)
    filter.predicate(params)(student(submission = Option(submission))) should be (true)

    a.originalityReport.overlap = Option(60)
    filter.predicate(params)(student(submission = Option(submission))) should be (true)
  }

  @Test def NotCheckedForPlagiarism(): Unit = {
    val filter = CourseworkFilters.NotCheckedForPlagiarism

    // Only applies to assignments that collect submissions and the department has plagiarism detection enabled
    department.plagiarismDetectionEnabled = false

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (false)

    department.plagiarismDetectionEnabled = true

    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when the submission exists and it doesn't have an originality report
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.hasOriginalityReport.booleanValue() should be (false)

    filter.predicate(student(submission = Option(submission))) should be (true)

    // Checked for plagiarism, no longer fits
    val a = new FileAttachment
    val originalityReport = new OriginalityReport
    originalityReport.reportReceived = true
    a.originalityReport = originalityReport

    submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

    submission.hasOriginalityReport.booleanValue() should be (true)

    filter.predicate(student(submission = Option(submission))) should be (false)
  }

  @Test def markedPlagiarised(): Unit = {
    val filter = CourseworkFilters.MarkedPlagiarised

    // Only applies to assignments that collect submissions
    assignment.collectSubmissions = false
    filter.applies(assignment) should be (false)

    assignment.collectSubmissions = true
    filter.applies(assignment) should be (true)

    // Valid when the submission exists and it has been marked as plagiarised
    filter.predicate(student(submission = None)) should be (false)

    val submission = Fixtures.submission("0672089", "cuscav")
    submission.assignment = assignment

    submission.plagiarismInvestigation = NotInvestigated

    filter.predicate(student(submission = Option(submission))) should be (false)

    submission.plagiarismInvestigation = SuspectPlagiarised

    filter.predicate(student(submission = Option(submission))) should be (true)

    submission.plagiarismInvestigation = InvestigationCompleted

    filter.predicate(student(submission = Option(submission))) should be (false)
  }

  @Test def NoFeedback(): Unit = {
    val filter = CourseworkFilters.NoFeedback

    // Should pass any assignment, so just check with null
    filter.applies(null) should be (true)

    // Valid where there's no feedback
    filter.predicate(student(feedback = None)) should be (true)

    val feedback = new Feedback
    feedback.actualMark = Option(41)
    feedback.assignment = assignment
    filter.predicate(student(feedback = Option(feedback))) should be (false)
  }

  @Test def FeedbackNotReleased(): Unit = {
    val filter = CourseworkFilters.FeedbackNotReleased

    // Should pass any assignment, so just check with null
    filter.applies(null) should be (true)

    // Valid where there's feedback, but it hasn't been released
    filter.predicate(student(feedback = None)) should be (false)

    val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
    feedback.actualMark = Option(41)
    feedback.released = false
    feedback.assignment = assignment

    filter.predicate(student(feedback = Option(feedback))) should be (true)

    feedback.released = true

    filter.predicate(student(feedback = Option(feedback))) should be (false)
  }

  @Test def FeedbackNotDownloaded(): Unit = {
    val filter = CourseworkFilters.FeedbackNotDownloaded

    // Should pass any assignment, so just check with null
    filter.applies(null) should be (true)

    val testFeedback = Fixtures.assignmentFeedback()
    testFeedback.actualMark = Option(41)
    testFeedback.assignment = assignment

    // Valid where there's feedback, but it hasn't been downloaded
    filter.predicate(student(feedback = None)) should be (false)
    filter.predicate(student(feedback = Option(testFeedback), feedbackDownloaded = false)) should be (true)
    filter.predicate(student(feedback = Option(testFeedback), feedbackDownloaded = true)) should be (false)
  }


}
