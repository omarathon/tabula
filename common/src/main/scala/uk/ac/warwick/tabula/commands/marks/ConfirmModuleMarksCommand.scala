package uk.ac.warwick.tabula.commands.marks

import enumeratum.{Enum, EnumEntry}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ConfirmModuleMarksCommand._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState._
import uk.ac.warwick.tabula.data.model.RecordedAssessmentComponentStudentMarkSource.ModuleMarkConfirmation
import uk.ac.warwick.tabula.data.model.RecordedModuleMarkSource.MarkConfirmation
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.marks.ConfirmedModuleMarkChangedNotification
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, AutowiringResitServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

sealed abstract class ConfirmModuleMarksAction(val label: String, val markState: MarkState, val comment: Option[String]) extends EnumEntry
object ConfirmModuleMarksAction extends Enum[ConfirmModuleMarksAction] {
  case object Confirm extends ConfirmModuleMarksAction("Confirmed", ConfirmedActual, None)
  case object AcademicIntegrity extends ConfirmModuleMarksAction("Unconfirmed (Ongoing academic conduct investigation)", UnconfirmedActual, Some("Unconfirmed due to ongoing academic conduct investigation"))
  case object Extension extends ConfirmModuleMarksAction("Unconfirmed (Deadline extended beyond BoE meeting)", UnconfirmedActual, Some("Unconfirmed due to deadline after the Board of Examiners meeting"))
  case object InternalStudentWBS extends ConfirmModuleMarksAction("Unconfirmed (Student internal to WBS)", UnconfirmedActual, Some("Unconfirmed, student is internal to WBS"))

  override def values: IndexedSeq[ConfirmModuleMarksAction] = findValues
}
class ConfirmModuleMarksActionConverter extends EnumTwoWayConverter(ConfirmModuleMarksAction)

object ConfirmModuleMarksCommand {
  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with ConfirmModuleMarksState
    with ConfirmModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with SelfValidating
  type SprCode = String

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser) =
    new ConfirmModuleMarksCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with ConfirmModuleMarksRequest
      with ConfirmModuleMarksValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with ModuleOccurrenceLoadModuleRegistrations
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with ComposableCommand[Result] // late-init due to ModuleOccurrenceUpdateMarksPermissions being called from permissions
      with RecordedModuleRegistrationsDescription
      with AutowiringProfileServiceComponent
      with ConfirmModuleMarkChangedCommandNotification
}

abstract class ConfirmModuleMarksCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result] with ConfirmModuleMarksState with ConfirmModuleMarksValidation {
  self: ConfirmModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent
    with RecordedModuleRegistrationsDescription =>

  override val mandatoryEventName: String = "ConfirmModuleMarks"

  def applyInternal(): Result = transactional() {
    studentsToConfirm.filterNot { case (module, _) => actions.asScala(module.sprCode) == ConfirmModuleMarksAction.InternalStudentWBS }.map { case (module, components) =>
      require(module.markState.forall(_ == UnconfirmedActual))

      val moduleRegistration = moduleRegistrations.find(_.sprCode == module.sprCode).get
      val recordedModuleRegistration = moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration(moduleRegistration)

      val action = actions.asScala(recordedModuleRegistration.sprCode)

      recordedModuleRegistration.addMark(
        uploader = currentUser.apparentUser,
        mark = module.mark,
        grade = module.grade,
        result = module.result,
        source = MarkConfirmation,
        comments = action.comment.orNull,
        markState = action.markState
      )

      // change the state of all components that are Unconfirmed actual (or that have no state)
      components.values.filter(c => c.markState.forall(_ == UnconfirmedActual) && !c.agreed).foreach { component =>
        val recordedAssessmentComponentStudent = assessmentComponentMarksService.getOrCreateRecordedStudent(component.upstreamAssessmentGroupMember)
        recordedAssessmentComponentStudent.addMark(
          uploader = currentUser.apparentUser,
          mark = component.mark,
          grade = component.grade,
          comments = null,
          source = ModuleMarkConfirmation,
          markState = action.markState
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
    actions.asScala.foreach { case (sprCode, action) =>
      if (action == null) {
        errors.rejectValue(s"actions[$sprCode]", "moduleMarks.confirm.noAction")
      } else if (action == ConfirmModuleMarksAction.Confirm) {
        val hasMissingModuleGrade = studentsWithMissingModuleGrade.exists(_._1.sprCode == sprCode)
        val hasMissingComponentGrade = studentsWithMissingComponentGrades.exists(_._1.sprCode == sprCode)

        if (hasMissingModuleGrade) {
          errors.rejectValue(s"actions[$sprCode]", "moduleMarks.confirm.missingGrade", Array(sprCode), "")
        } else if (hasMissingComponentGrade) {
          errors.rejectValue(s"actions[$sprCode]", "moduleMarks.confirm.missingComponentGrade", Array(sprCode), "")
        }
      }
    }
  }
}

trait ConfirmModuleMarksState extends ModuleOccurrenceState with ClearRecordedModuleMarksState {
  self: ModuleOccurrenceLoadModuleRegistrations =>

  lazy val studentModuleRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleMarkRecords.map { student =>
    moduleRegistrations.find(_.sprCode == student.sprCode).map { moduleRegistration =>
      student -> componentMarks(moduleRegistration).view.mapValues(_._1).toMap
    }.getOrElse(student -> Map.empty[AssessmentComponent, StudentMarkRecord])
  }

  lazy val studentsToConfirm: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter { case (module, _) =>
    module.markState.forall(_ == UnconfirmedActual) && !module.agreed
  }

  lazy val alreadyConfirmed: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.diff(studentsToConfirm)
    .filter { case (module, _) => module.markState.contains(ConfirmedActual) || module.markState.contains(Agreed) || module.agreed }

  lazy val studentsWithMissingModuleGrade: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter {
    case (module, _) => module.grade.isEmpty
  }

  lazy val studentsWithMissingComponentGrades: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, StudentMarkRecord])] = studentModuleRecords.filter {
    case (_, components) => components.values.exists(_.grade.isEmpty)
  }
}

trait ConfirmModuleMarksRequest {
  self: ConfirmModuleMarksState =>

  var actions: JMap[SprCode, ConfirmModuleMarksAction] = LazyMaps.create { sprCode: SprCode =>
    val hasMissingModuleGrade = studentsWithMissingModuleGrade.exists(_._1.sprCode == sprCode)
    val hasMissingComponentGrade = studentsWithMissingComponentGrades.exists(_._1.sprCode == sprCode)

    if (hasMissingModuleGrade && module.adminDepartment.rootDepartment.code == "ib") ConfirmModuleMarksAction.InternalStudentWBS
    else if (hasMissingModuleGrade || hasMissingComponentGrade) null: ConfirmModuleMarksAction
    else ConfirmModuleMarksAction.Confirm
  }.asJava
}

trait RecordedModuleRegistrationNotificationDepartment {
  self: ProfileServiceComponent =>

  //Pick up sub department of the department that owns the course linked to the StudentCourseDetails from the ModuleRegistration
  private def department(rmr: RecordedModuleRegistration): Option[Department] = {
      val student = profileService.getStudentCourseDetailsBySprCode(rmr.sprCode).head.student
      val module = rmr.moduleRegistration.map(_.module).get
      rmr.moduleRegistration
        .map(_.studentCourseDetails)
        .flatMap(_.course.department)
        .flatMap(_.subDepartmentsContaining(student).lastOption) // Get the sub-department containing the student, if applicable
        .filterNot(_.rootDepartment == module.adminDepartment.rootDepartment) // Make sure it isn't the same department as the one recording the module marks

  }

  def notificationDepartment(stuRecord: StudentModuleMarkRecord): Option[Department] =  {
    val marksHistory = stuRecord.history
    if (marksHistory.exists(_.markState == ConfirmedActual)) department(marksHistory.head.recordedModuleRegistration) else None
  }

  def notificationDepartment(rmr: RecordedModuleRegistration): Option[Department] = {
    val previousConfirmation = rmr.marks.tail.find(_.markState == ConfirmedActual)
    val currentMarks = rmr.marks.head

    if (previousConfirmation.exists(m => currentMarks.mark != m.mark || currentMarks.grade != m.grade || currentMarks.result != m.result)) department(rmr) else None
  }
}

trait ConfirmModuleMarkChangedCommandNotification extends RecordedModuleRegistrationNotificationDepartment with Notifies[Seq[RecordedModuleRegistration], Seq[RecordedModuleRegistration]] {

  self: ClearRecordedModuleMarksState with ProfileServiceComponent =>

  override def emit(result: Seq[RecordedModuleRegistration]): Seq[ConfirmedModuleMarkChangedNotification] = {
    val recordedModuleRegs: Seq[(RecordedModuleRegistration, Option[Department])] = result.map { rmr =>
      rmr ->  notificationDepartment(rmr)
    }

    recordedModuleRegs.filter(_._2.nonEmpty).groupBy(_._2.get).map { case (d, rmrWithDeptList) =>
      Notification.init(new ConfirmedModuleMarkChangedNotification, currentUser.apparentUser, rmrWithDeptList.map(_._1), d)
    }.toSeq
  }
}
