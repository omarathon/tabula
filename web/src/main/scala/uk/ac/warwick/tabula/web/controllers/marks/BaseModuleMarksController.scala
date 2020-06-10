package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.tabula.commands.marks.ModuleOccurrenceLoadModuleRegistrations
import uk.ac.warwick.tabula.commands.{MemberOrUser, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.web.controllers.BaseController

/**
 * Common base controller for module marks. Must have a RequestMapping with the following @PathVariables:
 *
 * - @PathVariable sitsModuleCode: String
 * - @PathVariable academicYear: AcademicYear
 * - @PathVariable occurrence: String
 *
 * Must also have a @ModelAttribute("command") which mixes in ModuleOccurrenceLoadModuleRegistrations
 */
abstract class BaseModuleMarksController extends BaseController
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringUserLookupComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("module")
  def module(@PathVariable sitsModuleCode: String): Module =
    mandatory(moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode))

  @ModelAttribute("membersBySprCode")
  def membersBySprCode(@ModelAttribute("command") command: ModuleOccurrenceLoadModuleRegistrations): Map[String, MemberOrUser] =
    command.moduleRegistrations.map { mr =>
      mr.sprCode -> MemberOrUser(Option(mr.studentCourseDetails).map(_.student), userLookup.getUserByWarwickUniId(SprCode.getUniversityId(mr.sprCode)))
    }.toMap

}
