package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.data.model.WorkflowCategory.SingleUse
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType
import uk.ac.warwick.tabula.services.AutowiringCM2MarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/coursework/admin/assignments/{assignment}/edit"))
class EditAssignmentDetailsController extends AbstractAssignmentController
  with AutowiringCM2MarkingWorkflowServiceComponent {

  type EditAssignmentDetailsCommand = EditAssignmentDetailsCommand.Command
  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def editAssignmentDetailsCommand(@PathVariable assignment: Assignment): EditAssignmentDetailsCommand =
    EditAssignmentDetailsCommand(mandatory(assignment))

  @RequestMapping
  def showForm(@ModelAttribute("command") cmd: EditAssignmentDetailsCommand, @PathVariable assignment: Assignment): Mav = {
    val module = assignment.module
    val canDeleteAssignment = !assignment.deleted && assignment.submissions.isEmpty && !assignment.hasReleasedFeedback
    val canDeleteMarkers = cmd.workflowCategory != SingleUse || cmd.workflow.exists(_.canDeleteMarkers)
    Mav("cm2/admin/assignments/edit_assignment_details",
      "department" -> module.adminDepartment,
      "module" -> module,
      "academicYear" -> cmd.academicYear,
      "reusableWorkflows" -> cm2MarkingWorkflowService.getReusableWorkflows(module.adminDepartment, academicYear),
      "availableWorkflows" -> MarkingWorkflowType.values.sorted,
      "allWorkflowCategories" -> WorkflowCategory.values,
      "workflow" -> cmd.workflow,
      "canEditWorkflowType" -> !assignment.isReleasedForMarking,
      // if the current workflow type isn't Single use then allow markers to be deleted if we switch to it
      "canDeleteMarkers" -> canDeleteMarkers,
      "canDeleteAssignment" -> canDeleteAssignment,
      "possibleAnonymityOptions" -> AssignmentAnonymity.values,
      "departmentAnonymity" -> (if (module.adminDepartment.showStudentName) AssignmentAnonymity.NameAndID else AssignmentAnonymity.IDOnly)
    ).crumbsList(Breadcrumbs.assignment(assignment))
  }

  @PostMapping(params = Array(ManageAssignmentMappingParameters.editAndAddFeedback, "action!=refresh", "action!=update, action=submit"))
  def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
    submit(cmd, errors, assignment, RedirectForce(Routes.admin.assignment.createOrEditFeedback(assignment, editMode)))

  @PostMapping
  def saveAndExit(@Valid @ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav = {
    submit(cmd, errors, assignment, Redirect(Routes.admin.assignment.submissionsandfeedback(assignment)))
  }

  private def submit(cmd: EditAssignmentDetailsCommand, errors: Errors, assignment: Assignment, mav: Mav): Mav =
    if (errors.hasErrors) showForm(cmd, assignment)
    else {
      cmd.apply()
      mav
    }
}
