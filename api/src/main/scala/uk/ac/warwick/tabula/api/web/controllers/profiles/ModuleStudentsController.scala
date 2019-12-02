package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/module/{module}/students"))
class ModuleStudentsController extends ApiController
  with GetModuleStudentsApi
  with AutowiringModuleRegistrationServiceComponent

trait GetModuleStudentsApi {

  self: ApiController with ModuleRegistrationServiceComponent =>

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable module: Module): ViewViewableCommand[Module] =
    new ViewViewableCommand(Permissions.Profiles.Read.ModuleRegistration.Core, mandatory(module))

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def currentSITSAcademicYear(@ModelAttribute("getCommand") cmd: ViewViewableCommand[Module]): Mav = {
    getMav(
      cmd.apply(),
      AcademicYear.now(),
      None,
      None
    )
  }

  @RequestMapping(path = Array("/{academicYear}"), method = Array(GET), produces = Array("application/json"))
  def specificAcademicYear(
    @ModelAttribute("getCommand") cmd: ViewViewableCommand[Module],
    @PathVariable academicYear: AcademicYear,
    @RequestParam(required = false) endDate: LocalDate,
    @RequestParam(required = false) occurrence: String
  ): Mav = {
    getMav(cmd.apply(), academicYear, Option(endDate), Option(occurrence))
  }

  def getMav(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Mav = {
    val usercodes = moduleRegistrationService.findRegisteredUsercodes(module, academicYear, endDate, occurrence)
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "usercodes" -> usercodes
    )))
  }
}
