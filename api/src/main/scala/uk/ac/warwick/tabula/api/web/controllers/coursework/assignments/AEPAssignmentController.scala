package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.servlet.http.HttpServletResponse
import org.joda.time.DateTime
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation._
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.AssignmentController._
import uk.ac.warwick.tabula.api.web.helpers._
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FileFormValue
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, AutowiringZipServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.web.Mav

object AEPAssignmentController {
  type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest with BindListener with SelfValidating
}

abstract class AEPAssignmentController extends ApiController
  with AssignmentToJsonConverter
  with AssessmentMembershipInfoToJsonConverter
  with AssignmentStudentToJsonConverter
  with ReplacingAssignmentStudentMessageResolver
  with GetAssignmentApiFullOutput

@Controller
@RequestMapping(value = Array("/v1/private/assignments/{assignment}"), params = Array("universityId"))
class AEPAssignmentCreateSubmissionController extends ApiController
  with AEPCreateSubmissionApi
  with SubmissionToJsonConverter
  with CreatesSubmission {
  validatesSelf[SelfValidating]
}

trait AEPCreateSubmissionApi {
  self: ApiController with SubmissionToJsonConverter with CreatesSubmission =>

  @ModelAttribute("createCommand")
  def command(@PathVariable assignment: Assignment,
    @RequestParam("universityId") member: Member,
    @RequestParam("submittedDate") submittedDate: DateTime,
    @RequestParam("submissionDeadline") submissionDeadline: DateTime): SubmitAssignmentCommand
    with ComposableCommand[Submission] with SubmitAssignmentBinding with SubmitAssignmentSetSubmittedDatePermissions
    with SubmitAssignmentDescription with SubmitAssignmentValidation with SubmitAssignmentNotifications with SubmitAssignmentTriggers
    with AutowiringSubmissionServiceComponent with AutowiringFeaturesComponent with AutowiringZipServiceComponent with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent =
    SubmitAssignmentCommand.onBehalfOfWithSubmittedDateAndDeadline(assignment, member, submittedDate, submissionDeadline)

  // Two ways into this - either uploading files in advance to the attachments API or submitting a multipart request
  @RequestMapping(method = Array(POST), consumes = Array("multipart/mixed"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  def create(@RequestPart("submission") request: CreateSubmissionRequest, @RequestPart("attachments") files: JList[MultipartFile], @ModelAttribute("createCommand") command: SubmitAssignmentCommand, errors: BindingResult)(implicit response: HttpServletResponse): Mav = {
    request.copyTo(command, errors)

    command.assignment.attachmentField.map(_.id).foreach { fieldId =>
      command.fields.get(fieldId).asInstanceOf[FileFormValue].file.upload.addAll(files)
    }

    doCreate(command, globalValidator, errors)
  }

  @RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  def create(@RequestBody request: CreateSubmissionRequest, @ModelAttribute("createCommand") command: SubmitAssignmentCommand, errors: BindingResult)(implicit response: HttpServletResponse): Mav = {
    request.copyTo(command, errors)

    doCreate(command, globalValidator, errors)
  }

}
