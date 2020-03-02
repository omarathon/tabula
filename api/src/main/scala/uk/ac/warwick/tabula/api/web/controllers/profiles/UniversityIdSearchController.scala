package uk.ac.warwick.tabula.api.web.controllers.profiles

import java.util.Optional

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.commands.profiles.{UniversityIdSearchCommand, UserSearchCommandState}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/v1/universityIdSearch"))
class UniversityIdSearchController extends AbstractUserSearchController {

  @ModelAttribute("getCommand")
  override protected def getCommand(@PathVariable academicYear: Optional[AcademicYear], user: CurrentUser): Appliable[Seq[String]] =
    UniversityIdSearchCommand(academicYear.orElse(AcademicYear.now()), user)

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