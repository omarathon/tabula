package uk.ac.warwick.tabula.web.controllers.cm2

import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{DownloadExtensionAttachmentCommand, DownloadExtensionAttachmentState, RequestExtensionCommand, RequestExtensionCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException, PermissionDeniedException}

@Controller
@RequestMapping(value = Array("/coursework/assignment/{assignment}/extension"))
class RequestExtensionController extends CourseworkController {

  type ExtensionRequestCommand = Appliable[Extension] with RequestExtensionCommandState

  var profileService: ProfileService = Wire.auto[ProfileService]

  @ModelAttribute("command")
  def cmd(
    @PathVariable assignment: Assignment,
    @RequestParam(defaultValue = "") action: String,
    user: CurrentUser
  ): ExtensionRequestCommand = RequestExtensionCommand(assignment, user, action)

  validatesSelf[SelfValidating]

  @RequestMapping
  def showForm(@ModelAttribute("command") cmd: ExtensionRequestCommand): Mav = {
    val assignment = cmd.assignment

    if (!assignment.module.adminDepartment.allowExtensionRequests) {
      logger.info("Rejecting access to extension request screen as department does not allow extension requests")
      throw PermissionDeniedException(user, Permissions.Extension.MakeRequest, assignment)
    } else {
      val existingExtension = assignment.approvedExtensions.get(user.userId)

      val existingRequest = assignment.currentExtensionRequests.get(user.userId)
      existingRequest.orElse(existingExtension).foreach(cmd.presetValues)

      val profile = profileService.getMemberByUser(user.apparentUser)

      // is this an edit of an existing request
      val isModification = existingRequest.exists(!_.isManual)

      Mav("cm2/submit/extension_request",
        "profile" -> profile,
        "module" -> assignment.module,
        "assignment" -> assignment,
        "department" -> assignment.module.adminDepartment,
        "isModification" -> isModification,
        "existingExtension" -> existingExtension,
        "existingRequest" -> existingRequest,
        "command" -> cmd,
        "returnTo" -> getReturnTo(Routes.cm2.assignment(assignment))
      )
    }
  }

  @PostMapping
  def persistExtensionRequest(@Valid @ModelAttribute("command") cmd: ExtensionRequestCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      showForm(cmd)
    } else {
      val extension = cmd.apply()
      Mav(
        "cm2/submit/extension_request_success",
        "isReply" -> extension.moreInfoReceived,
        "assignment" -> cmd.assignment
      )
    }
  }

}

@Controller
@RequestMapping(value = Array("/coursework/assignment/{assignment}/extension/supporting-file/{filename}"))
class RequestExtensionSupportingFileController extends CourseworkController {

  type DownloadAttachmentCommand = Appliable[Option[RenderableAttachment]] with DownloadExtensionAttachmentState

  @ModelAttribute("downloadAttachmentCommand")
  def attachmentCommand(
    @PathVariable assignment: Assignment,
    @PathVariable filename: String,
    user: CurrentUser
  ): DownloadAttachmentCommand = {
    val existingExtension = assignment.approvedExtensions.get(user.userId)
    val existingRequest = assignment.currentExtensionRequests.get(user.userId)
    val extension = existingRequest.orElse(existingExtension)

    DownloadExtensionAttachmentCommand(mandatory(extension), mandatory(filename))
  }

  @RequestMapping
  def supportingFile(@ModelAttribute("downloadAttachmentCommand") attachmentCommand: DownloadAttachmentCommand): RenderableFile =
    attachmentCommand.apply()
      .getOrElse(throw new ItemNotFoundException)

}
