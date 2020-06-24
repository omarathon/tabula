package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ConfirmModuleMarksCommand.{Comment, Result, SprCode}
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState.{ConfirmedActual, UnconfirmedActual}
import uk.ac.warwick.tabula.data.model.RecordedAssessmentComponentStudentMarkSource.ModuleMarkConfirmation
import uk.ac.warwick.tabula.data.model.RecordedModuleMarkSource.MarkConfirmation
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.marks.ConfirmModuleMarkChangedNotification
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object ConfirmModuleMarksCommand {

  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with ConfirmModuleMarksState
    with ConfirmModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with SelfValidating
  type SprCode = String
  type Comment = String

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser) =
    new ConfirmModuleMarksCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with ConfirmModuleMarksRequest
      with ConfirmModuleMarksValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with ModuleOccurrenceLoadModuleRegistrations
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with ComposableCommand[Result] // late-init due to ModuleOccurrenceUpdateMarksPermissions being called from permissions
      with ModuleOccurrenceDescription
      with AutowiringProfileServiceComponent
      with ConfirmModuleMarkChangedCommandNotification
}


class ConfirmModuleMarksCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result] with ConfirmModuleMarksState with ConfirmModuleMarksValidation {

  self: ConfirmModuleMarksRequest with ModuleOccurrenceLoadModuleRegistrations with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent =>

  val mandatoryEventName: String = "ConfirmModuleMarks"

  def applyInternal(): Result = transactional() {

    studentsToConfirm.map { case (module, components) =>

      require(module.markState.forall(_ == UnconfirmedActual))

      val moduleRegistration = moduleRegistrations.find(_.sprCode == module.sprCode).get
      val recordedModuleRegistration = moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration(moduleRegistration)

      recordedModuleRegistration.addMark(
        uploader = currentUser.apparentUser,
        mark = module.mark,
        grade = module.grade,
        result = module.result,
        source = MarkConfirmation,
        comments = comments.asScala(recordedModuleRegistration.sprCode),
        markState = ConfirmedActual
      )

      // change the state of all components that are Unconfirmed actual (or that have no state)
      components.values.filter(c => c.markState.forall(_ == UnconfirmedActual)).map { component =>

        val recordedAssessmentComponentStudent = assessmentComponentMarksService.getOrCreateRecordedStudent(component.upstreamAssessmentGroupMember)
        recordedAssessmentComponentStudent.addMark(
          uploader = currentUser.apparentUser,
          mark = component.mark,
          grade = component.grade,
          comments = component.history.headOption.map(_.comments).orNull, // leave component comments as-is
          source = ModuleMarkConfirmation,
          markState = ConfirmedActual
        )

        assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)
      }

      moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)
      recordedModuleRegistration
    }
  }
}

trait ConfirmModuleMarksValidation extends SelfValidating {
  self: ConfirmModuleMarksState with ConfirmModuleMarksRequest =>

  def validate(errors: Errors): Unit = {

    // TODO - is validation on grades even needed :superthinking:
    // cannot confirm module marks if any components or modules are missing a grade (marks may be missing but indicator grades should always be present)
    lazy val studentsWithMissingModuleGrade: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter {
      case (module, _) => module.grade.isEmpty
    }

    lazy val studentsWithMissingComponentGrades: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter {
      case (_, components) => components.values.exists(_.grade.isEmpty)
    }

    if (studentsWithMissingModuleGrade.nonEmpty)
      errors.reject("moduleMarks.confirm.missingGrade", Array(studentsWithMissingModuleGrade.map(_._1.sprCode).mkString(", ")), "")

    if (studentsWithMissingComponentGrades.nonEmpty)
      errors.reject("moduleMarks.confirm.missingComponentGrade", Array(studentsWithMissingComponentGrades.map(_._1.sprCode).mkString(", ")), "")
  }
}

trait ConfirmModuleMarksState extends ModuleOccurrenceState with ClearRecordedModuleMarksState {

  self: ModuleOccurrenceLoadModuleRegistrations =>

  lazy val studentModuleRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleMarkRecords.map { student =>
    moduleRegistrations.find(_.sprCode == student.sprCode).map { moduleRegistration =>
      student -> componentMarks(moduleRegistration.studentCourseDetails.student.universityId)
    }.getOrElse(student -> Map.empty[AssessmentComponent, StudentMarkRecord])
  }

  lazy val studentsToConfirm: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter { case (module, _) =>
    module.markState.forall(_ == UnconfirmedActual)
  }

  lazy val alreadyConfirmed: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.diff(studentsToConfirm)
    .filter { case (module, _) => module.markState.isDefined && module.markState.contains(ConfirmedActual) }
}

trait ConfirmModuleMarksRequest {
  self: ConfirmModuleMarksState =>

  var comments: JMap[SprCode, Comment] = LazyMaps.create { sprCode: SprCode =>
    studentModuleRecords.map(_._1).find(_.sprCode == sprCode).flatMap(_.history.headOption).map(_.comments).orNull
  }.asJava

}


case class RecordedModuleRegistrationInfo(recordedModuleRegistration: RecordedModuleRegistration, member: StudentMember)

trait ConfirmModuleMarkChangedCommandNotification extends Notifies[Seq[RecordedModuleRegistration], Seq[RecordedModuleRegistration]] {

  self: ClearRecordedModuleMarksState with ProfileServiceComponent =>

  //Pick up sub department of the department that owns the course linked to the StudentCourseDetails from the ModuleRegistration
  def notificationDepartment(rmr: RecordedModuleRegistration): Option[Department] =  {
    val student = profileService.getStudentCourseDetailsBySprCode(rmr.sprCode).head.student
    val module = rmr.moduleRegistration.map(_.module).get
    rmr.moduleRegistration
      .map(_.studentCourseDetails)
      .flatMap(_.course.department)
      .flatMap(_.subDepartmentsContaining(student).lastOption) // Get the sub-department containing the student, if applicable
      .filterNot(_.rootDepartment == module.adminDepartment.rootDepartment) // Make sure it isn't the same department as the one recording the module marks
  }

  override def emit(result: Seq[RecordedModuleRegistration]) = {

    val recordedModuleRegs: Seq[(RecordedModuleRegistration, Option[Department])] = result.filter(rmr => rmr.marks.tail.exists(m => m.markState == ConfirmedActual)).map { rmr =>
      rmr ->  notificationDepartment(rmr)
    }
    recordedModuleRegs.filter(_._2.nonEmpty).groupBy(_._2.get).map { case (d, rmrWithDeptList) =>
      Notification.init(new ConfirmModuleMarkChangedNotification, currentUser.apparentUser, rmrWithDeptList.map(_._1), d)
    }.toSeq

  }
}
