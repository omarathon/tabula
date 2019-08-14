package uk.ac.warwick.tabula.commands.exams.exams

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, UniversityId}

import scala.collection.JavaConverters._


trait ExamState extends UpdatesStudentMembership {

  self: AssessmentServiceComponent with UserLookupComponent with HasAcademicYear with SpecifiesGroupType
    with AssessmentMembershipServiceComponent =>

  val updateStudentMembershipGroupIsUniversityIds: Boolean = false
  // bind variables
  var name: String = _

  def exam: Exam = null

  def module: Module

  def academicYear: AcademicYear

  var markingWorkflow: MarkingWorkflow = _

  // TAB-3597
  lazy val allMarkingWorkflows: Seq[MarkingWorkflow] = (exam match {
    case existing: Exam if Option(existing.markingWorkflow).exists(_.department != module.adminDepartment) =>
      module.adminDepartment.markingWorkflows ++ Seq(existing.markingWorkflow)
    case _ =>
      module.adminDepartment.markingWorkflows
  }).filter(_.validForExams)

  def copyTo(exam: Exam) {
    exam.assessmentMembershipService = assessmentMembershipService
    exam.name = name
    exam.markingWorkflow = markingWorkflow

    exam.assessmentGroups.clear()
    exam.assessmentGroups.addAll(assessmentGroups)
    for (group <- assessmentGroups.asScala if group.exam == null) {
      group.exam = exam
    }
    exam.members.copyFrom(members)
  }

}

trait ExamValidation extends SelfValidating {

  self: ExamState with AssessmentServiceComponent with UserLookupComponent =>

  override def validate(errors: Errors) {

    if (!name.hasText) {
      errors.rejectValue("name", "exam.name.empty")
    } else {
      val duplicates = assessmentService.getExamByNameYearModule(name, academicYear, module).filterNot { existing => existing eq exam }
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "exam.name.duplicate", Array(name), "")
      }
    }

    def isValidUniID(userString: String) = {
      UniversityId.isValid(userString) && userLookup.getUserByWarwickUniId(userString).isFoundUser
    }

    def isValidUserCode(userString: String) = {
      val user = userLookup.getUserByUserId(userString)
      user.isFoundUser && null != user.getWarwickId
    }

    val invalidUserStrings = massAddUsersEntries.filterNot(userString => isValidUniID(userString) || isValidUserCode(userString))
    if (invalidUserStrings.nonEmpty) {
      errors.rejectValue("massAddUsers", "userString.notfound.specified", Array(invalidUserStrings.mkString(", ")), "")
    }
  }
}

trait ModifiesExamMembership extends UpdatesStudentMembership with SpecifiesGroupType {
  self: ExamState with HasAcademicYear with UserLookupComponent with AssessmentMembershipServiceComponent =>

  lazy val existingGroups: Option[Seq[UpstreamAssessmentGroupInfo]] = Option(exam).map(_.upstreamAssessmentGroupInfos)
  lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = None // Needs to be none as we're using massAddUsers for existing members

  def updateAssessmentGroups() {
    assessmentGroups = upstreamGroups.asScala.flatMap(ug => {
      val template = new AssessmentGroup
      template.assessmentComponent = ug.assessmentComponent
      template.occurrence = ug.occurrence
      template.exam = exam
      assessmentMembershipService.getAssessmentGroup(template) orElse Some(template)
    }).distinct.asJava
  }

  // TAB-3507 - Filter to show type E only
  override lazy val availableUpstreamGroups: Seq[UpstreamGroup] = {
    val allAssessmentComponents = assessmentMembershipService.getAssessmentComponents(module)
    val examAssessmentComponents = {
      if (allAssessmentComponents.exists(_.assessmentType == AssessmentType.Exam)) {
        allAssessmentComponents.filter(_.assessmentType == AssessmentType.Exam)
      } else {
        allAssessmentComponents
      }
    }
    for {
      ua <- examAssessmentComponents
      uagInfo <- assessmentMembershipService.getUpstreamAssessmentGroupInfo(ua, academicYear)
    } yield new UpstreamGroup(ua, uagInfo.upstreamAssessmentGroup, uagInfo.currentMembers)
  }

}
