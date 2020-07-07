package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.marks.MarksExportToSitsQueueCommand.Result
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

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

abstract class MarksExportToSitsQueueCommandInternal extends CommandInternal[Result] with TaskBenchmarking {
  self: AssessmentComponentMarksServiceComponent
    with ModuleAndDepartmentServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional(readOnly = true) {
    val allComponentMarksNeedsWritingToSits = benchmarkTask("Load the component mark queue") {
      assessmentComponentMarksService.allNeedingWritingToSits(filtered = false).filterNot(_.marks.isEmpty)
    }

    type UniversityId = String
    type TabulaModuleCode = String
    type SitsModuleCode = String
    type Occurrence = String

    val allModules: Map[TabulaModuleCode, Module] = benchmarkTask("Load all modules for component marks") {
      moduleAndDepartmentService.getModulesByCodes(
        allComponentMarksNeedsWritingToSits.map(_.moduleCode).distinct.flatMap(Module.stripCats).map(_.toLowerCase)
      ).map(module => module.code -> module).toMap
    }

    val (componentMarksCanUploadToSitsForYear, componentMarksCannotUploadToSitsForYear) = benchmarkTask("Filter component marks with upload disabled") {
      allComponentMarksNeedsWritingToSits.partition { student =>
        Module.stripCats(student.moduleCode).map(_.toLowerCase).flatMap(allModules.get).forall { module =>
          module.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, module)
        }
      }
    }

    val allModuleRegistrations: Map[UniversityId, Map[(SitsModuleCode, AcademicYear, Occurrence), Seq[ModuleRegistration]]] = benchmarkTask("Load all module registrations for component marks") {
      moduleRegistrationService.getByUniversityIds(componentMarksCanUploadToSitsForYear.map(_.universityId).distinct, includeDeleted = false)
        .groupBy(mr => SprCode.getUniversityId(mr.sprCode))
        .map { case (sprCode, registrations) =>
          sprCode -> registrations.groupBy(mr => (mr.sitsModuleCode, mr.academicYear, mr.occurrence))
        }
    }

    val (componentMarksCanUploadAgreedMarksToSits, componentMarksCannotUploadAgreedMarksToSits) = benchmarkTask("Filter component marks not allowing agreed mark processing") {
      componentMarksCanUploadToSitsForYear
        .map { student =>
          // We can't restrict this by AssessmentGroup because it might be a resit mark by another mechanism
          lazy val moduleRegistrations: Seq[ModuleRegistration] =
            allModuleRegistrations
              .getOrElse(student.universityId, Map.empty)
              .getOrElse((student.moduleCode, student.academicYear, student.occurrence), Seq.empty)

          student -> moduleRegistrations
        }
        .partition { case (student, moduleRegistrations) =>
          // true if latestState is empty (which should never be the case anyway)
          student.latestState.forall { markState =>
            markState != MarkState.Agreed || moduleRegistrations.exists { moduleRegistration =>
              MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails), MarkState.MarkUploadTime)
            }
          }
        }
    }

    val allModuleMarksNeedsWritingToSits = benchmarkTask("Load the module mark queue") {
      moduleRegistrationMarksService.allNeedingWritingToSits(filtered = false).filterNot(_.marks.isEmpty)
    }

    val (moduleMarksCanUploadToSitsForYear, moduleMarksCannotUploadToSitsForYear) = benchmarkTask("Filter module marks with upload disabled") {
      allModuleMarksNeedsWritingToSits.partition { student =>
        student.moduleRegistration.map(_.module).exists(m => m.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, m))
      }
    }

    val (moduleMarksCanUploadAgreedMarksToSits, moduleMarksCannotUploadAgreedMarksToSits) = benchmarkTask("Filter module marks not allowing agreed mark processing") {
      moduleMarksCanUploadToSitsForYear.partition { student =>
        // true if latestState is empty (which should never be the case anyway)
        student.latestState.forall { markState =>
          markState != MarkState.Agreed || student.moduleRegistration.exists { moduleRegistration =>
            MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails), MarkState.MarkUploadTime)
          }
        }
      }
    }

    benchmarkTask("Build result") {
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
}

trait MarksExportToSitsQueuePermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.MarksManagement)
  }
}
