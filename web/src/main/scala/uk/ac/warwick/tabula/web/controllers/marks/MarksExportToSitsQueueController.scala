package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{MarkState, ModuleRegistration, RecordedAssessmentComponentStudent, RecordedModuleRegistration}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.marks.MarksExportToSitsQueueCommand.Result

@Controller
@RequestMapping(Array("/marks/sits-queue"))
class MarksExportToSitsQueueController extends BaseController {
  @ModelAttribute("command")
  def command: MarksExportToSitsQueueCommand.Command = MarksExportToSitsQueueCommand()

  @RequestMapping
  def render(@ModelAttribute("command") command: MarksExportToSitsQueueCommand.Command): Mav =
    Mav("marks/sits-queue", "queue" -> command.apply())
}

object MarksExportToSitsQueueCommand {
  type SprCode = String
  case class Result(
    componentMarksNotEnabledForYear: Int,
    componentMarksAgreedNotAllowed: Int,
    componentMarksQueue: Seq[(SprCode, RecordedAssessmentComponentStudent)],

    moduleMarksNotEnabledForYear: Int,
    moduleMarksAgreedNotAllowed: Int,
    moduleMarksQueue: Seq[RecordedModuleRegistration],
  )
  type Command = Appliable[Result]

  def apply(): Command =
    new MarksExportToSitsQueueCommandInternal
      with ComposableCommand[Result]
      with MarksExportToSitsQueuePermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringTransactionalComponent
      with Unaudited with ReadOnly
}

abstract class MarksExportToSitsQueueCommandInternal extends CommandInternal[Result] {
  self: AssessmentComponentMarksServiceComponent
    with ModuleAndDepartmentServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional(readOnly = true) {
    val allComponentMarksNeedsWritingToSits = assessmentComponentMarksService.allNeedingWritingToSits.filterNot(_.marks.isEmpty)

    val (componentMarksCanUploadToSitsForYear, componentMarksCannotUploadToSitsForYear) =
      allComponentMarksNeedsWritingToSits.partition { student =>
        moduleAndDepartmentService.getModuleBySitsCode(student.moduleCode).forall { module =>
          module.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, module)
        }
      }

    val (componentMarksCanUploadAgreedMarksToSits, componentMarksCannotUploadAgreedMarksToSits) =
      componentMarksCanUploadToSitsForYear
        .map { student =>
          // We can't restrict this by AssessmentGroup because it might be a resit mark by another mechanism
          lazy val moduleRegistrations: Seq[ModuleRegistration] =
            moduleRegistrationService.getByModuleOccurrence(student.moduleCode, student.academicYear, student.occurrence)
              .filter(_.studentCourseDetails.student.universityId == student.universityId)

          student -> moduleRegistrations
        }
        .partition { case (student, moduleRegistrations) =>
          // true if latestState is empty (which should never be the case anyway)
          student.latestState.forall { markState =>
            markState != MarkState.Agreed || moduleRegistrations.exists { moduleRegistration =>
              MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails))
            }
          }
        }

    val allModuleMarksNeedsWritingToSits = moduleRegistrationMarksService.allNeedingWritingToSits.filterNot(_.marks.isEmpty)

    val (moduleMarksCanUploadToSitsForYear, moduleMarksCannotUploadToSitsForYear) =
      allModuleMarksNeedsWritingToSits.partition { student =>
        student.moduleRegistration.map(_.module).exists(m => m.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, m))
      }

    val (moduleMarksCanUploadAgreedMarksToSits, moduleMarksCannotUploadAgreedMarksToSits) =
      moduleMarksCanUploadToSitsForYear.partition { student =>
        // true if latestState is empty (which should never be the case anyway)
        student.latestState.forall { markState =>
          markState != MarkState.Agreed || student.moduleRegistration.exists { moduleRegistration =>
            MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails))
          }
        }
      }

    Result(
      componentMarksNotEnabledForYear = componentMarksCannotUploadToSitsForYear.size,
      componentMarksAgreedNotAllowed = componentMarksCannotUploadAgreedMarksToSits.size,
      componentMarksQueue =
        componentMarksCanUploadAgreedMarksToSits
          .map { case (student, moduleRegistrations) =>
            moduleRegistrations.headOption.map(_.sprCode).getOrElse(s"${student.universityId}/???") -> student
          }
          .sortBy { case (sprCode, student) =>
            (sprCode, student.moduleCode, student.academicYear, student.occurrence, student.sequence, student.resitSequence)
          },

      moduleMarksNotEnabledForYear = moduleMarksCannotUploadToSitsForYear.size,
      moduleMarksAgreedNotAllowed = moduleMarksCannotUploadAgreedMarksToSits.size,
      moduleMarksQueue =
        moduleMarksCanUploadAgreedMarksToSits
          .sortBy { student =>
            (student.sprCode, student.sitsModuleCode, student.academicYear, student.occurrence)
          },
    )
  }
}

trait MarksExportToSitsQueuePermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.MarksManagement)
  }
}
