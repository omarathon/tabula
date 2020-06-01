package uk.ac.warwick.tabula.commands.mitcircs

import java.io.{File, InputStream}
import java.nio.charset.StandardCharsets

import com.google.common.io.{CharSource, Files}
import enumeratum.{Enum, EnumEntry}
import org.joda.time.{DateTime, DateTimeConstants, DateTimeUtils, ReadableInstant}
import org.springframework.validation.{BeanPropertyBindingResult, Errors}
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.MitCircsDataGenerationCommand._
import uk.ac.warwick.tabula.commands.mitcircs.submission.{AffectedAssessmentItem, CreateMitCircsSubmissionCommand, EditMitCircsSubmissionCommand, MitCircsAffectedAssessmentsCommand}
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitCircsContact, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.{AssessmentType, Department, StudentMember}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.permissions.{Permissions, ScopelessPermission}
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesOfficerRoleDefinition
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.workingdays.{WorkingDaysHelper, WorkingDaysHelperImpl}

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.{Random => SRandom}

object MitCircsDataGenerationCommand {
  type Result = Seq[MitigatingCircumstancesSubmission]
  type Command = Appliable[Result] with MitCircsDataGenerationState with SelfValidating
  val RequiredPermission: ScopelessPermission = Permissions.ImportSystemData

  def withFakeTime[A](when: ReadableInstant)(fn: => A): A =
    try {
      DateTimeUtils.setCurrentMillisFixed(when.getMillis)
      fn
    } finally {
      DateTimeUtils.setCurrentMillisSystem()
    }

  def apply(department: Department): Command =
    new MitCircsDataGenerationCommandInternal(department)
      with ComposableCommand[Result]
      with MitCircsDataGenerationRequest
      with MitCircsDataGenerationPermissions
      with MitCircsDataGenerationDescription
      with MitCircsDataGenerationValidation
      with AutowiringProfileServiceComponent
      with AutowiringPermissionsServiceComponent
}

abstract class MitCircsDataGenerationCommandInternal(val department: Department)
  extends CommandInternal[Result]
    with MitCircsDataGenerationState
    with MitCircsDataGenerationDataLoading {
  self: MitCircsDataGenerationRequest
    with ProfileServiceComponent
    with PermissionsServiceComponent =>

  private def dummyWords(words: Int): String = {
    val paragraphCount = words / dummyText.split(' ').length
    val wordCount = words % dummyText.split(' ').length

    val wordsPara = dummyText.split(' ').slice(0, wordCount).mkString(" ")

    val paras = dummyParagraphs(paragraphCount)

    s"$paras\n\n$wordsPara"
  }

  private def dummyParagraphs(paragraphs: Int): String =
    (1 to paragraphs).map(_ => dummyText).mkString("\n\n")

  private def randomPastDateTime(maximumDaysInPast: Int = maximumDaysInPast, base: DateTime = DateTime.now, workingHoursOnly: Boolean = false): DateTime = {
    var dt =
      base.minusDays(random.nextInt(maximumDaysInPast + 1))
        .withMinuteOfHour(random.nextInt(60))
        .withSecondOfMinute(random.nextInt(60))

    if (workingHoursOnly) {
      // Shift to a workday if it's not one already
      while (dt.getDayOfWeek == DateTimeConstants.SATURDAY || dt.getDayOfWeek == DateTimeConstants.SUNDAY || workingDays.getHolidayDates.contains(dt.toLocalDate.asJava) || dt.isAfter(base)) {
        dt = dt.plusDays(random.nextInt(7))
        if (dt.isAfter(base))
          dt = dt.minusDays(random.nextInt(maximumDaysInPast))
      }

      dt.withHourOfDay(9 + random.nextInt(8))
    } else {
      // Avoid early mornings (DST transition)
      dt.withHourOfDay(4 + random.nextInt(20))
    }
  }

  private def randomFutureDateTime(maximumDaysInFuture: Int = maximumDaysInFuture, base: DateTime = DateTime.now, workingHoursOnly: Boolean = false): DateTime = {
    var dt =
      base.plusDays(random.nextInt(maximumDaysInFuture + 1))
        .withMinuteOfHour(random.nextInt(60))
        .withSecondOfMinute(random.nextInt(60))

    if (workingHoursOnly) {
      // Shift to a workday if it's not one already
      while (dt.getDayOfWeek == DateTimeConstants.SATURDAY || dt.getDayOfWeek == DateTimeConstants.SUNDAY || workingDays.getHolidayDates.contains(dt.toLocalDate.asJava) || dt.isBefore(base)) {
        dt = dt.plusDays(random.nextInt(7))
        if (dt.isAfter(base.plusDays(maximumDaysInFuture)))
          dt = dt.minusDays(random.nextInt(maximumDaysInFuture))
      }

      dt.withHourOfDay(9 + random.nextInt(8))
    } else {
      // Avoid early mornings (DST transition)
      dt.withHourOfDay(4 + random.nextInt(20))
    }
  }

  private def randomEnum[A <: EnumEntry](enum: Enum[A]): A = enum.values(random.nextInt(enum.values.size))
  private def randomSeq[A](seq: Seq[A]): A = seq(random.nextInt(seq.size))

  private def withinRate(rate: Double): Boolean = {
    require(rate <= 1.0)
    (rate / random.nextDouble()).toInt > 0
  }

  private def rateRange(rate: Double, max: Int = 10): Range =
    1 to Math.min((rate / random.nextDouble()).toInt, max)

  private def randomMitCircsSubmission(student: StudentMember, user: User): MitigatingCircumstancesSubmission = withFakeTime(randomPastDateTime()) {
    val command = CreateMitCircsSubmissionCommand(student, user)
    command.startDate = randomPastDateTime().toLocalDate

    if (withinRate(ongoingSubmissionRate)) {
      command.noEndDate = true
    } else {
      command.endDate = randomFutureDateTime(maximumDaysInFuture = maximumDaysInPast, base = command.startDate.toDateTimeAtCurrentTime).toLocalDate
    }

    val validIssueTypes = IssueType.validIssueTypes(student)

    command.issueTypes.addAll(
      (rateRange(additionalIssueTypesRate).map(_ => randomSeq(validIssueTypes)).toSet + randomSeq(validIssueTypes)).asJavaCollection
    )

    if (command.issueTypes.asScala.contains(IssueType.Other)) {
      command.issueTypeDetails = dummyWords(random.nextInt(5) + 3)
    }

    command.reason = dummyWords(random.nextInt(500) + 50)

    val affectedAssessments = {
      val cmd = MitCircsAffectedAssessmentsCommand(student)
      cmd.startDate = command.startDate
      cmd.endDate = command.endDate
      cmd.apply()
    }
    command.affectedAssessments.addAll(
      affectedAssessments.flatMap { a =>
        if (withinRate(upstreamAffectedAssessmentRate)) {
          Some {
            val item = new AffectedAssessmentItem
            item.moduleCode = a.assessmentComponent.moduleCode
            item.module = a.module
            item.sequence = a.assessmentComponent.sequence
            item.academicYear = a.academicYear
            item.name = a.name
            item.assessmentType = a.assessmentComponent.assessmentType
            item.deadline = a.deadline.getOrElse(randomFutureDateTime(maximumDaysInFuture = maximumDaysInPast, base = command.startDate.toDateTimeAtCurrentTime).toLocalDate)
            item
          }
        } else None
      }.asJavaCollection
    )

    val moduleRegistrations =
      student.moduleRegistrationsByYear(Some(AcademicYear.now()))
        .filter { mr => mr.agreedMark.isEmpty && mr.agreedGrade.isEmpty }

    if (moduleRegistrations.nonEmpty) {
      command.affectedAssessments.addAll(
        rateRange(additionalAffectedAssessmentRate).map { _ =>
          val item = new AffectedAssessmentItem
          item.moduleCode = randomSeq(moduleRegistrations.toSeq).module.code
          item.academicYear = AcademicYear.now()
          item.name = dummyWords(random.nextInt(5) + 3)
          item.assessmentType = randomSeq(Seq(AssessmentType.Essay, AssessmentType.SummerExam))
          item.deadline = randomFutureDateTime(maximumDaysInFuture = maximumDaysInPast, base = command.startDate.toDateTimeAtCurrentTime).toLocalDate
          item
        }.asJavaCollection
      )
    }

    command.contacts.addAll(
      rateRange(additionalContactRate).map(_ => randomEnum(MitCircsContact)).toSet.asJavaCollection
    )

    if (command.contacts.asScala.contains(MitCircsContact.Other)) {
      command.contactOther = dummyWords(random.nextInt(3) + 1)
    }

    if (command.contacts.isEmpty) {
      command.contacted = false
      command.noContactReason = dummyWords(random.nextInt(100) + 10)
    } else {
      command.contacted = true
    }

    rateRange(additionalAttachmentRate).foreach { _ =>
      val in = CharSource.wrap(dummyWords(random.nextInt(1000) + 100)).asByteSource(StandardCharsets.UTF_8)
      val filename = s"${random.alphanumeric.take(random.nextInt(10) + 3).mkString}.txt"

      command.file.upload.add(new MultipartFile {
        override def getName: String = filename
        override def getOriginalFilename: String = filename
        override def getContentType: String = "text/plain"
        override def isEmpty: Boolean = false
        override def getSize: Long = in.size()
        override def getBytes: Array[Byte] = in.read()
        override def getInputStream: InputStream = in.openStream()
        override def transferTo(dest: File): Unit = in.copyTo(Files.asByteSink(dest))
      })
    }

    if (withinRate(pendingEvidenceRate) || command.file.upload.isEmpty) {
      command.pendingEvidence = dummyWords(random.nextInt(200) + 100)
      command.pendingEvidenceDue = randomFutureDateTime(base = DateTime.now().plusDays(1)).toLocalDate
    }

    val bindingResult = new BeanPropertyBindingResult(command, "command")
    command.onBind(bindingResult)
    command.validate(bindingResult)

    if (bindingResult.hasErrors) {
      throw new IllegalStateException(bindingResult.toString)
    }

    command.apply()
  }

  override def applyInternal(): Result =
    students.flatMap { student =>
      val selfCreatedSubmissions =
        rateRange(selfSubmissionRate).map(_ => randomMitCircsSubmission(student, student.asSsoUser))
          .sortBy(_.createdDate)

      // Link to a previous submission if we have more than one
      if (selfCreatedSubmissions.size > 1) withFakeTime(randomFutureDateTime(base = selfCreatedSubmissions.last.lastModified)) {
        val command = EditMitCircsSubmissionCommand(selfCreatedSubmissions.last, student.asSsoUser)
        command.relatedSubmission = selfCreatedSubmissions.head
        command.apply()
      }

      // Approve the submissions
      selfCreatedSubmissions.foreach { submission =>
        if (withinRate(selfSubmissionApprovalRate)) withFakeTime(randomFutureDateTime(base = submission.lastModified)) {
          val command = EditMitCircsSubmissionCommand(submission, student.asSsoUser)
          command.approve = true
          command.apply()
        }
      }

      val mcoCreatedSubmissions =
        rateRange(behalfSubmissionRate).map(_ => randomMitCircsSubmission(student, randomSeq(mitigatingCircumstancesOfficers.toSeq)))

      // Approve the submissions
      mcoCreatedSubmissions.foreach { submission =>
        if (withinRate(behalfSubmissionApprovalRate)) withFakeTime(randomFutureDateTime(base = submission.lastModified)) {
          val command = EditMitCircsSubmissionCommand(submission, student.asSsoUser)
          command.approve = true
          command.apply()
        }
      }

      val submissions = selfCreatedSubmissions ++ mcoCreatedSubmissions

      submissions.filter(_.state == MitigatingCircumstancesSubmissionState.Submitted).foreach { submission =>
        // Add messages
        (1 to random.nextInt(averageMessagesPerSubmission * 2)).foreach { i =>
          withFakeTime(randomFutureDateTime(base = submission.createdDate, maximumDaysInFuture = i)) {
            val isStudent = i % 2 == 0

            val command = SendMessageCommand(submission, if (isStudent) student.asSsoUser else randomSeq(mitigatingCircumstancesOfficers.toSeq))
            command.message = dummyWords(random.nextInt(200) + 100)

            rateRange(messageAttachmentRate).foreach { _ =>
              val in = CharSource.wrap(dummyWords(random.nextInt(1000) + 100)).asByteSource(StandardCharsets.UTF_8)
              val filename = s"${random.alphanumeric.take(random.nextInt(10) + 3).mkString}.txt"

              command.file.upload.add(new MultipartFile {
                override def getName: String = filename
                override def getOriginalFilename: String = filename
                override def getContentType: String = "text/plain"
                override def isEmpty: Boolean = false
                override def getSize: Long = in.size()
                override def getBytes: Array[Byte] = in.read()
                override def getInputStream: InputStream = in.openStream()
                override def transferTo(dest: File): Unit = in.copyTo(Files.asByteSink(dest))
              })
            }

            val bindingResult = new BeanPropertyBindingResult(command, "command")
            command.onBind(bindingResult)
            command.validate(bindingResult)

            if (bindingResult.hasErrors) {
              throw new IllegalStateException(bindingResult.toString)
            }

            command.apply()
          }
        }

        // Add notes
        (1 to random.nextInt(averageNotesPerSubmission * 2)).foreach { i =>
          withFakeTime(randomFutureDateTime(base = submission.createdDate, maximumDaysInFuture = i)) {
            val command = AddMitCircSubmissionNoteCommand(submission, randomSeq(mitigatingCircumstancesOfficers.toSeq))
            command.text = dummyWords(random.nextInt(200) + 100)

            rateRange(messageAttachmentRate).foreach { _ =>
              val in = CharSource.wrap(dummyWords(random.nextInt(1000) + 100)).asByteSource(StandardCharsets.UTF_8)
              val filename = s"${random.alphanumeric.take(random.nextInt(10) + 3).mkString}.txt"

              command.file.upload.add(new MultipartFile {
                override def getName: String = filename
                override def getOriginalFilename: String = filename
                override def getContentType: String = "text/plain"
                override def isEmpty: Boolean = false
                override def getSize: Long = in.size()
                override def getBytes: Array[Byte] = in.read()
                override def getInputStream: InputStream = in.openStream()
                override def transferTo(dest: File): Unit = in.copyTo(Files.asByteSink(dest))
              })
            }

            val bindingResult = new BeanPropertyBindingResult(command, "command")
            command.onBind(bindingResult)
            command.validate(bindingResult)

            if (bindingResult.hasErrors) {
              throw new IllegalStateException(bindingResult.toString)
            }

            command.apply()
          }
        }
      }

      submissions
    }
}

trait MitCircsDataGenerationPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsDataGenerationState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission)
}

trait MitCircsDataGenerationDescription extends Describable[Result] {
  self: MitCircsDataGenerationState =>

  override lazy val eventName: String = "MitCircsDataGeneration"

  override def describe(d: Description): Unit =
    d.department(department)
}

trait MitCircsDataGenerationValidation extends SelfValidating {
  self: MitCircsDataGenerationState
    with MitCircsDataGenerationRequest
    with MitCircsDataGenerationDataLoading
    with ProfileServiceComponent
    with PermissionsServiceComponent =>

  override def validate(errors: Errors): Unit = {
    // Ensure the sandbox data for the department exists
    if (!SandboxData.Departments.contains(department.code)) {
      errors.reject("mitigatingCircumstances.dataGeneration.invalidDepartment")
    }

    // Ensure that the department has at least one MCO
    if (mitigatingCircumstancesOfficers.isEmpty) {
      errors.reject("mitigatingCircumstances.dataGeneration.noMitigatingCircumstancesOfficers")
    }
  }
}

trait MitCircsDataGenerationRequest {
  /**
    * Proportion of issues that are ongoing
    */
  var ongoingSubmissionRate: Double = 0.2

  /**
    * The chance that the submission will get an additional issue type (called multiple times)
    */
  var additionalIssueTypesRate: Double = 0.8

  /**
    * Proportion of upstream affected assessments that are selected
    */
  var upstreamAffectedAssessmentRate: Double = 0.2

  /**
    * The chance that an additional affected assessment will be included
    */
  var additionalAffectedAssessmentRate: Double = 0.2

  /**
    * The change that the submission will get an additional contact
    */
  var additionalContactRate: Double = 0.7

  /**
    * The rate that the submission will get an additional piece of evidence
    */
  var additionalAttachmentRate: Double = 0.8

  /**
    * The proportion of submissions that will provide future evidence
    */
  var pendingEvidenceRate: Double = 0.2

  /**
    * Proportion of students who will make a draft submission (may be multiple)
    */
  var selfSubmissionRate: Double = 0.25

  /**
    * Proportion of self submissions that are eventually submitted
    */
  var selfSubmissionApprovalRate: Double = 0.5

  /**
    * Proportion of students who will have a submission made on their behalf
    */
  var behalfSubmissionRate: Double = 0.1

  /**
    * Proportion of submissions made on behalf of a student that are then submitted by the student
    */
  var behalfSubmissionApprovalRate: Double = 0.9

  /**
    * The average number of messages between the MCO and the student
    */
  var averageMessagesPerSubmission: Int = 6

  /**
    * The proportion of messages that will have an attachment (may have multiple)
    */
  var messageAttachmentRate: Double = 0.1

  /**
    * The average number of private notes per submission
    */
  var averageNotesPerSubmission: Int = 10

  /**
    * How many days in the past events may have occurred
    */
  var maximumDaysInPast = 180

  /**
    * How many days in the future events may occur
    */
  var maximumDaysInFuture = 30

  var dummyText: String =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut " +
    "labore et dolore magna aliqua. Lut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
    "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit " +
    "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt " +
    "in culpa qui officiae deserunt mollit anim id est laborum."

  val random = new SRandom
}

trait MitCircsDataGenerationState {
  def department: Department
}

trait MitCircsDataGenerationDataLoading {
  self: MitCircsDataGenerationState
    with ProfileServiceComponent
    with PermissionsServiceComponent =>

  // Get all the students for the department
  lazy val students: Seq[StudentMember] = SandboxData.Departments(department.code).routes.values.flatMap { route =>
    (route.studentsStartId to route.studentsEndId).flatMap { universityId =>
      profileService.getMemberByUniversityId(universityId.toString)
    }
  }.toSeq.collect { case s: StudentMember => s }

  // MCOs for the department
  lazy val mitigatingCircumstancesOfficers: Set[User] =
    permissionsService.ensureUserGroupFor(department, MitigatingCircumstancesOfficerRoleDefinition)
      .users

  lazy val workingDays: WorkingDaysHelper = new WorkingDaysHelperImpl
}
