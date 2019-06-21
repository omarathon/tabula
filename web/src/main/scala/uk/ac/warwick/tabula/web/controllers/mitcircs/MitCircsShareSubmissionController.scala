package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsShareSubmissionCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController

import scala.collection.JavaConverters._

@Controller
@RequestMapping(value = Array("/profiles/view/{student}/personalcircs/mitcircs/share/{submission}"))
class MitCircsShareSubmissionController extends AbstractViewProfileController
  with AutowiringUserLookupComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("addCommand")
  def addCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
  ): MitCircsShareSubmissionCommand.AddCommand = {
    mustBeLinked(submission, student)
    MitCircsShareSubmissionCommand.add(submission, user.apparentUser)
  }

  @ModelAttribute("removeCommand")
  def removeCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
  ): MitCircsShareSubmissionCommand.RemoveCommand = {
    mustBeLinked(submission, student)
    MitCircsShareSubmissionCommand.remove(submission, user.apparentUser)
  }

  @ModelAttribute("role")
  def role: RoleDefinition = MitCircsShareSubmissionCommand.roleDefinition

  @RequestMapping
  def form(@PathVariable student: StudentMember): Mav =
    Mav("mitcircs/submissions/share")
      .crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)

  @PostMapping(params = Array("_command=add"))
  def addSharing(
    @Valid @ModelAttribute("addCommand") command: MitCircsShareSubmissionCommand.AddCommand,
    errors: Errors,
    @PathVariable student: StudentMember
  ): Mav =
    if (errors.hasErrors) {
      form(student)
    } else {
      command.apply()
      form(student).addObjects(
        "users" -> userLookup.getUsersByUserIds(command.usercodes).asScala,
        "action" -> "add"
      )
    }

  @PostMapping(params = Array("_command=remove"))
  def removeSharing(
    @Valid @ModelAttribute("removeCommand") command: MitCircsShareSubmissionCommand.RemoveCommand,
    errors: Errors,
    @PathVariable student: StudentMember
  ): Mav =
    if (errors.hasErrors) {
      form(student)
    } else {
      command.apply()
      form(student).addObjects(
        "users" -> userLookup.getUsersByUserIds(command.usercodes).asScala,
        "action" -> "remove"
      )
    }


}
