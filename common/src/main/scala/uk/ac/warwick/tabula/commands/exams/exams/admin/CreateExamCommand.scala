package uk.ac.warwick.tabula.commands.exams.exams.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JList, _}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.exams.admin.CreateExamCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._


object CreateExamCommand {

  type Result = Exam
  type Command = Appliable[Result] with CreateExamState with CreateExamRequest with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.Exam.Create

  def apply(module: Module, academicYear: AcademicYear) = new CreateExamCommandInternal(module, academicYear)
    with ComposableCommand[Result]
    with CreateExamRequest
    with CreateExamValidation
    with CreateExamPermissions
    with CreateExamDescription
    with AutowiringExamServiceComponent
    with AutowiringAssessmentMembershipServiceComponent
    with AutowiringUserLookupComponent
}

class CreateExamCommandInternal(val module: Module, val academicYear: AcademicYear) extends CommandInternal[Result]
  with CreateExamState with CreateExamValidation with PopulateOnForm {

  self: CreateExamRequest with ExamServiceComponent with AssessmentMembershipServiceComponent with UserLookupComponent =>

  override def populate(): Unit = {
    // if there is exactly one assessment component link it by default
    assessmentComponents = availableComponents.filter(_.inUse).filter(_.assessmentType.subtype == TabulaAssessmentSubtype.Exam) match {
      case exam :: Nil => JArrayList(exam)
      case _ => JArrayList()
    }

    // link the first available assessment component by default
    occurrences = JArrayList(availableOccurrences.headOption.toSeq)
  }

  def applyInternal(): Result = transactional() {
    val firstComponent = assessmentComponents.asScala.head
    val name = firstComponent.examPaperTitle.getOrElse(firstComponent.name)
    val exam = new Exam(name, module.adminDepartment, academicYear)
    for (component <- assessmentComponents.asScala; occurrence <- occurrences.asScala) {
      val assessmentGroup = new AssessmentGroup
      assessmentGroup.assessmentComponent = component
      assessmentGroup.occurrence = occurrence
      assessmentGroup.exam = exam
      exam.assessmentGroups.add(assessmentGroup)
    }

    examService.save(exam)
  }
}

trait CreateExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateExamState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, module)
  }
}

trait CreateExamValidation extends SelfValidating {
  self: CreateExamRequest with CreateExamState =>

  def validate(errors: Errors) {
    if (assessmentComponents.isEmpty) errors.rejectValue("assessmentComponents", "exam.assessmentComponents.empty")
    if (occurrences.isEmpty) errors.rejectValue("occurrences", "exam.occurrence.empty")
  }
}

trait CreateExamDescription extends Describable[Result] {
  self: CreateExamState =>

  def describe(d: Description) {
    d.module(module)
  }

  override def describeResult(d: Description, exam: Exam) {
    d.exam(exam).module(module)
  }
}

trait CreateExamState {

  self: ExamServiceComponent with AssessmentMembershipServiceComponent with UserLookupComponent =>

  def module: Module
  def academicYear: AcademicYear

  lazy val availableComponents: Seq[AssessmentComponent] = assessmentMembershipService.getAssessmentComponents(module, inUseOnly = false)

  lazy val assessmentGroups: Map[AssessmentComponent, Seq[UpstreamAssessmentGroupInfo]] = availableComponents
    .map(c => c -> assessmentMembershipService.getUpstreamAssessmentGroupInfo(c, academicYear)).toMap

  lazy val availableOccurrences: Seq[String] = assessmentGroups.values.flatten.map(_.upstreamAssessmentGroup.occurrence).toSeq.distinct.sorted

  lazy val students: Map[UniversityId, User] = userLookup.usersByWarwickUniIds(
    assessmentGroups.values.flatten.flatMap(_.currentMembers).map(_.universityId).toSeq.distinct
  )

  lazy val existingExams: Seq[Exam] = examService.getExamsByDepartment(module.adminDepartment, academicYear)
}

trait CreateExamRequest {
  var occurrences: JList[String] = JArrayList()
  var assessmentComponents: JList[AssessmentComponent] = JArrayList()
  var showNotInUse: Boolean = false
  var showNonExam: Boolean = false
}