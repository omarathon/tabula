package uk.ac.warwick.tabula.commands.marks

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.GenerateModuleResitsCommand.{ResitItem, Result, Sequence, SprCode}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState.Agreed
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, GradeBoundary, GradeBoundaryProcess, Module, RecordedResit}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._
import scala.util.Try

case class StudentMarks (
  module: StudentModuleMarkRecord,
  requiresResit: Boolean,
  incrementsAttempt: Boolean,
  components: Map[AssessmentComponent, StudentMarkRecord],
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
    def this(sprCode: String, sequence: String, weighting: String) {
      this()
      this.sprCode = sprCode
      this.sequence = sequence
      this.weighting = weighting
    }

    var sprCode: SprCode = _
    var sequence: String = _
    var create: Boolean = _
    var assessmentType: String = AssessmentType.SeptemberExam.astCode // defaults to september exam
    var weighting: String = _
  }
}


class GenerateModuleResitsCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result] with GenerateModuleResitsState with GenerateModuleResitsValidation {

  self: GenerateModuleResitsRequest with ModuleOccurrenceLoadModuleRegistrations with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  val mandatoryEventName: String = "GenerateModuleResits"

  def applyInternal(): Result = transactional() {
    resitsToCreate.flatMap { case (sprCode, resits) =>

      val studentMarks: Option[StudentMarks] = requiresResits.find(_.module.sprCode == sprCode)

      val components: Iterable[(AssessmentComponent, StudentMarkRecord)] = studentMarks.map(_.components).getOrElse(Nil)

      resits.flatMap { case (sequence, resitItem) =>
        val componentMarks = components.find(_._1.sequence == sequence).map(_._2)
        componentMarks.map { cm =>
          val recordedResit = new RecordedResit(cm, sprCode)
          recordedResit.assessmentType = resitItem.assessmentType
          recordedResit.weighting = resitItem.weighting.toInt
          recordedResit.currentResitAttempt = {
            val currentAttempt = cm.currentResitAttempt.getOrElse(1)
            if (studentMarks.exists(_.incrementsAttempt)) currentAttempt + 1 else currentAttempt
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

    for (
      (sprCode, resits) <- resitsToCreate;
      (sequence, resit) <- resits
    ) {
      if (!resit.weighting.hasText) {
        errors.rejectValue(s"resits[$sprCode][$sequence].weighting", "moduleMarks.resit.weighting.missing")
      } else if (resit.weighting.toIntOption.isEmpty) {
        errors.rejectValue(s"resits[$sprCode][$sequence].weighting", "moduleMarks.resit.weighting.nonInt")
      }
    }
  }
}

trait GenerateModuleResitsState extends ModuleOccurrenceState {

  self: ModuleOccurrenceLoadModuleRegistrations with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  def currentUser: CurrentUser

  private lazy val gradeBoundaries: Seq[GradeBoundary] = (moduleRegistrations.map(_.marksCode) ++ studentComponentMarkRecords.map(_._1.marksCode))
    .distinct.flatMap(assessmentMembershipService.markScheme)

  def getGradeBoundary(marksCode: String, process: GradeBoundaryProcess, grade: Option[String]): Option[GradeBoundary] =
    gradeBoundaries.find { gb => gb.marksCode == marksCode && gb.process == process && grade.contains(gb.grade) }

  lazy val existingResits: Seq[RecordedResit] = upstreamAssessmentGroupInfos.flatMap { info => resitService.getAllResits(info.upstreamAssessmentGroup) }

  lazy val requiresResits: Seq[StudentMarks] = studentModuleMarkRecords.filter(_.markState.contains(Agreed)).flatMap { student =>
    val moduleRegistration = moduleRegistrations.find(_.sprCode == student.sprCode)
    val components = moduleRegistration.map(mr => componentMarks(mr.studentCourseDetails.student.universityId))
      .getOrElse(Map.empty[AssessmentComponent, StudentMarkRecord])

    val process = if (moduleRegistration.exists(_.currentResitAttempt.nonEmpty)) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
    val gradeBoundary = moduleRegistrations.find(_.sprCode == student.sprCode).flatMap { mr => getGradeBoundary(mr.marksCode, process, student.grade) }

    if (gradeBoundary.exists(_.generatesResit)) {
      Some(StudentMarks(student, gradeBoundary.exists(_.generatesResit), gradeBoundary.exists(_.incrementsAttempt), components))
    } else {
      None
    }
  }

}

trait GenerateModuleResitsRequest {
  self: GenerateModuleResitsState with ModuleOccurrenceLoadModuleRegistrations =>

  var resits: JMap[SprCode, JMap[Sequence, ResitItem]] = LazyMaps.create { sprcode: SprCode =>
    LazyMaps.create { sequence: Sequence =>
      val weighting: Int = assessmentComponents.find(_.sequence == sequence).map(_.rawWeighting.toInt).getOrElse(0)
      new ResitItem(sprcode, sequence, weighting.toString)
    }.asJava
  }.asJava

  def resitsToCreate: Map[SprCode, Map[Sequence, ResitItem]]  = resits.asScala.view.mapValues(_.asScala.toMap.filter(_._2.create)).toMap

}
