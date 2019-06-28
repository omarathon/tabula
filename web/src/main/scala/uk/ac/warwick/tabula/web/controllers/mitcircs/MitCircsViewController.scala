package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsShareSubmissionCommand
import uk.ac.warwick.tabula.commands.mitcircs.{ReviewMitCircsSubmissionCommand, StudentViewMitCircsSubmissionCommand}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.roles.{MitigatingCircumstancesViewer, RoleBuilder}
import uk.ac.warwick.tabula.services.permissions.AutowiringRoleServiceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/profiles/view/{student}/personalcircs/mitcircs/view/{submission}"))
class MitCircsViewController extends AbstractViewProfileController with AutowiringRoleServiceComponent {

  @ModelAttribute("command")
  def getCommand(
    @PathVariable student: StudentMember,
    @PathVariable submission: MitigatingCircumstancesSubmission
  ): StudentViewMitCircsSubmissionCommand.Command = {
    mustBeLinked(submission, student)
    StudentViewMitCircsSubmissionCommand(submission, user.apparentUser)
  }

  @RequestMapping
  def render(@ModelAttribute("command") cmd: StudentViewMitCircsSubmissionCommand.Command, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    val submission = cmd.apply()

    // TAB-7293 If the current user has review permissions bounce them to the review page unless the state would allow them to edit it
    if (
      // Not editable by the current user (i.e. not created on behalf of the student)
      !submission.isEditable(user.apparentUser)

      // Not explicitly granted the viewer role (i.e. sharing)
      && !roleService.hasRole(user, RoleBuilder.build(MitCircsShareSubmissionCommand.roleDefinition, Some(submission), MitCircsShareSubmissionCommand.roleDefinition.getName))

      // Has review permissions
      && securityService.can(user, ReviewMitCircsSubmissionCommand.RequiredPermission, submission)) {
      Redirect(Routes.mitcircs.Admin.review(submission))
    } else {
      Mav("mitcircs/view",
        "submission" -> submission,
        "isSelf" -> user.universityId.maybeText.contains(submission.student.universityId)
      ).crumbs(breadcrumbsStudent(activeAcademicYear, submission.student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
    }
  }

}
