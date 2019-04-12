package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.RenderMitCircsAttachmentCommand
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.mitcircs.submission._
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

import scala.collection.immutable.ListMap

abstract class AbstractMitCircsFormController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("registeredModules")
  def registeredModules(@PathVariable student: StudentMember): ListMap[AcademicYear, Seq[Module]] = {
    val builder = ListMap.newBuilder[AcademicYear, Seq[Module]]

    student.moduleRegistrationsByYear(None)
      .filter { mr => Option(mr.agreedMark).isEmpty && Option(mr.agreedGrade).isEmpty }
      .groupBy(_.academicYear)
      .mapValues(_.map(_.module).toSeq.sorted)
      .toSeq
      .sortBy { case (year, _) => -year.startYear }
      .foreach { case (year, modules) => builder += year -> modules }

    builder.result()
  }

}

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}/new"))
class MitCircsSubmissionController extends AbstractMitCircsFormController {

  type CreateCommand = Appliable[MitigatingCircumstancesSubmission] with CreateMitCircsSubmissionState with SelfValidating

  @ModelAttribute("command") def create(@PathVariable student: StudentMember, user: CurrentUser): CreateCommand =
    CreateMitCircsSubmissionCommand(mandatory(student), user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("command") cmd: CreateCommand, @PathVariable student: StudentMember): Mav = {
    Mav("mitcircs/submissions/form", Map("issueTypes" -> IssueType.values))
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("command") cmd: CreateCommand, errors: Errors, @PathVariable student: StudentMember): Mav = {
    if (errors.hasErrors) form(cmd, student)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Student.home(student))
    }
  }
}

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}/edit/{submission}"))
class MitCircsEditController extends AbstractMitCircsFormController {

  type EditCommand = Appliable[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with SelfValidating

  @ModelAttribute("command") def edit(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    user: CurrentUser
  ): EditCommand = {
    mustBeLinked(submission, student)
    EditMitCircsSubmissionCommand(mandatory(submission), user.apparentUser)
  }

  @RequestMapping
  def form(@ModelAttribute("command") cmd: EditCommand, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/submissions/form", Map("issueTypes" -> IssueType.values))
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("command") cmd: EditCommand, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    if (errors.hasErrors) form(cmd, submission)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Student.home(submission.student))
    }
  }

}

@Controller
@RequestMapping(Array("/mitcircs/profile/{student}/{submission}/supporting-file/{filename}"))
class MitCircsAttachmentController extends BaseController {

  type RenderAttachmentCommand = Appliable[Option[RenderableAttachment]]

  @ModelAttribute("renderAttachmentCommand")
  def attachmentCommand(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    @PathVariable filename: String
  ): RenderAttachmentCommand = {
    mustBeLinked(submission, student)
    RenderMitCircsAttachmentCommand(mandatory(submission), mandatory(filename))
  }

  @RequestMapping(method = Array(GET))
  def supportingFile(@ModelAttribute("renderAttachmentCommand") attachmentCommand: RenderAttachmentCommand, @PathVariable filename: String): RenderableFile =
    attachmentCommand.apply().getOrElse(throw new ItemNotFoundException())

}

