package uk.ac.warwick.tabula.commands.marks

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.GenerateModuleResitsCommand.{ResitItem, Result, Sequence, SprCode}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, GradeBoundary, Module, RecordedResit}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._
import scala.util.Try

case class ComponentMarks (
  component: StudentMarkRecord,
  existingResit: Option[RecordedResit],
  requiresResit: Boolean,
  incrementsAttempt: Boolean
)

case class StudentMarks (
  module: StudentModuleMarkRecord,
  components: Map[AssessmentComponent, ComponentMarks],
)

object GenerateModuleResitsCommand {

  type Result = Seq[RecordedResit]
  type Command = Appliable[Result]
    with GenerateModuleResitsState
    with GenerateModuleResitsRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with SelfValidating
  type SprCode = String
  type Sequence = String


  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser) =
    new GenerateModuleResitsCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with GenerateModuleResitsRequest
      with GenerateModuleResitsValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with ModuleOccurrenceLoadModuleRegistrations
      with AutowiringResitServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with ComposableCommand[Result] // late-init due to ModuleOccurrenceUpdateMarksPermissions being called from permissions
      with ModuleOccurrenceDescription[Result]

  class ResitItem {
    def this(sprCode: String, sequence: String) {
      this()
      this.sprCode = sprCode
      this.sequence = sequence
    }

    var sprCode: SprCode = _
    var sequence: String = _
    var create: Boolean = _
    var assessmentType: String = AssessmentType.SeptemberExam.astCode // defaults to september exam
  }
}


class GenerateModuleResitsCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result] with GenerateModuleResitsState with GenerateModuleResitsValidation {

  self: GenerateModuleResitsRequest with ModuleOccurrenceLoadModuleRegistrations with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  val mandatoryEventName: String = "GenerateModuleResits"

  def applyInternal(): Result = transactional() {
    resitsToCreate.flatMap { case (sprCode, resits) =>

      val studentMarkRecords: Iterable[(AssessmentComponent, ComponentMarks)] = requiresResits.find(_.module.sprCode == sprCode).map(_.components).getOrElse(Map())

      val highestResitSequence: Int = studentMarkRecords.flatMap(_._2.component.resitSequence)
        .map(_.replaceAll("[^0-9]", "")) // strip out any characters
        .flatMap(s => Try(s.toInt).toOption)
        .maxByOption(identity)
        .getOrElse(0)

      resits.zipWithIndex.flatMap { case ((sequence, resitItem), index) =>
        val componentMarks = studentMarkRecords.find(_._1.sequence == sequence).map(_._2)
        componentMarks.map { cm =>
          val recordedResit = new RecordedResit(cm.component, sprCode)
          recordedResit.assessmentType = resitItem.assessmentType
          recordedResit.resitSequence = f"${highestResitSequence + index + 1}%03d" // 3 chars padded with leading zeros
          recordedResit.currentResitAttempt = {
            val currentAttempt = cm.component.currentResitAttempt.getOrElse(1)
            if(cm.incrementsAttempt) currentAttempt + 1 else currentAttempt
          }
          recordedResit.needsWritingToSits = true
          recordedResit.updatedBy = currentUser.apparentUser
          recordedResit.updatedDate = DateTime.now
          resitService.saveOrUpdate(recordedResit)
        }
      }
    }.toSeq
  }
}

trait GenerateModuleResitsValidation extends SelfValidating {
  self: GenerateModuleResitsState with GenerateModuleResitsRequest =>

  def validate(errors: Errors): Unit = {

  }
}

trait GenerateModuleResitsState extends ModuleOccurrenceState {

  self: ModuleOccurrenceLoadModuleRegistrations with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  def currentUser: CurrentUser

  private lazy val gradeBoundaries: Seq[GradeBoundary] = (moduleRegistrations.map(_.marksCode) ++ studentComponentMarkRecords.map(_._1.marksCode))
    .distinct.flatMap(assessmentMembershipService.markScheme)

  def getGradeBoundary(marksCode: String, process: String, grade: Option[String]): Option[GradeBoundary] =
    gradeBoundaries.find { gb => gb.marksCode == marksCode && gb.process == process && grade.contains(gb.grade) }

  lazy val existingResits: Seq[RecordedResit] = upstreamAssessmentGroupInfos.flatMap { info =>resitService.getAllResits(info.upstreamAssessmentGroup) }

  lazy val requiresResits: Seq[StudentMarks] = studentModuleMarkRecords.flatMap { student =>
    val moduleRegistration = moduleRegistrations.find(_.sprCode == student.sprCode)
    val components = moduleRegistration.map(mr => componentMarks(mr.studentCourseDetails.student.universityId))
      .getOrElse(Map.empty[AssessmentComponent, StudentMarkRecord])
    val process = if (components.exists(_._2.resitExpected)) "RAS" else "SAS"
    val gradeBoundary = moduleRegistrations.find(_.sprCode == student.sprCode).flatMap { mr => getGradeBoundary(mr.marksCode, process, student.grade) }

    val componentsWithResitInfo = components.map { case (ac, marks) =>
      val process = if (marks.resitExpected) "RAS" else "SAS"
      val gradeBoundary = getGradeBoundary(ac.marksCode, process, marks.grade)
      val existingResit = existingResits.filter(r => r.sprCode == student.sprCode && r.sequence == ac.sequence).sortBy(_.resitSequence).headOption
      ac -> ComponentMarks(marks, existingResit, gradeBoundary.exists(_.generatesResit), gradeBoundary.exists(_.incrementsAttempt))
    }

    if (gradeBoundary.exists(_.generatesResit)) Some(StudentMarks(student, componentsWithResitInfo))
    else None
  }


}

trait GenerateModuleResitsRequest {
  self: GenerateModuleResitsState =>

  var resits: JMap[SprCode, JMap[Sequence, ResitItem]] = LazyMaps.create { sprcode: SprCode =>
    LazyMaps.create { sequence: Sequence => new ResitItem(sprcode, sequence) }.asJava
  }.asJava

  def resitsToCreate: Map[SprCode, Map[Sequence, ResitItem]]  = resits.asScala.view.mapValues(_.asScala.toMap.filter(_._2.create)).toMap

}
