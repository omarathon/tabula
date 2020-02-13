package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileSubsetCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/profiles/view/subset/{student}"))
class ViewProfileSubsetController extends ProfilesController {

  @ModelAttribute("command")
  def getViewProfileSubsetCommand(@PathVariable student: User): ViewProfileSubsetCommand.Command =
    ViewProfileSubsetCommand(student, user)

  @RequestMapping
  def viewProfile(@ModelAttribute("command") command: ViewProfileSubsetCommand.Command): Mav = {
    val profileSubset = command.apply()

    Mav("profiles/profile/view_subset",
      "isMember" -> profileSubset.isMember,
      "studentUser" -> profileSubset.user,
      "profile" -> profileSubset.profile,
      "studentCourseDetails" -> profileSubset.courseDetails
    ).noLayout()
  }

}
