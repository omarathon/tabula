package uk.ac.warwick.tabula.api.web.controllers.profiles

import java.util.Optional

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.api.commands.profiles.{UserCodeSearchCommand, UserSearchCommandState}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/v1/usercodeSearch"))
class UserCodeSearchController extends AbstractUserSearchController {

  @ModelAttribute("getCommand")
  override protected def getCommand(@PathVariable academicYear: Optional[AcademicYear], user: CurrentUser): Appliable[Seq[String]] =
    UserCodeSearchCommand(academicYear.orElse(AcademicYear.now()), user)

  override protected val resultKey: String = "usercodes"

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
