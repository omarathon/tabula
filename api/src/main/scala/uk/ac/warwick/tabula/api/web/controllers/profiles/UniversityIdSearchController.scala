package uk.ac.warwick.tabula.api.web.controllers.profiles

import java.util.Optional

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.commands.profiles.{UniversityIdSearchCommand, UserCodeSearchCommand, UserSearchCommandState}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/universityIdSearch"))
class UniversityIdSearchController extends AbstractUserSearchController {

  @ModelAttribute("getCommand")
  override protected def getCommand(@PathVariable academicYear: Optional[AcademicYear]): Appliable[Seq[String]] =
    UniversityIdSearchCommand(academicYear.orElse(AcademicYear.now()))

  override protected val resultKey: String = "universityIds"

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def search(
    @ModelAttribute("getCommand") command: Appliable[Seq[String]] with UserSearchCommandState
  ): Mav = {
    doSearch(command)
  }

  @RequestMapping(value = Array("/{academicYear}"), method = Array(GET), produces = Array("application/json"))
  def searchWithYear(
    @ModelAttribute("getCommand") command: Appliable[Seq[String]] with UserSearchCommandState
  ): Mav = {
    doSearch(command)
  }
}