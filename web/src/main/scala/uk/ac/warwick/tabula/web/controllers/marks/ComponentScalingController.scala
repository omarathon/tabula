package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.ComponentScalingCommand
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

@Controller
@RequestMapping(Array("/marks/admin/assessment-component/{assessmentComponent}/{upstreamAssessmentGroup}/scaling"))
class ComponentScalingController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable assessmentComponent: AssessmentComponent, @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): ComponentScalingCommand.Command =
    ComponentScalingCommand(assessmentComponent, upstreamAssessmentGroup, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable assessmentComponent: AssessmentComponent, @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): Seq[BreadCrumb] = {
    val department = assessmentComponent.module.adminDepartment
    val academicYear = upstreamAssessmentGroup.academicYear

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.AssessmentComponents(department, academicYear),
      MarksBreadcrumbs.Admin.AssessmentComponentScaling(assessmentComponent, upstreamAssessmentGroup, active = true),
    )
  }

  private val formView: String = "marks/admin/assessment-components/scaling"

  // We run validation when showing the form so we can avoid people clicking the button
  @RequestMapping
  def showForm(@Valid @ModelAttribute("command") cmd: ComponentScalingCommand.Command, errors: Errors): String = {
    formView
  }

  @PostMapping(params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("command") cmd: ComponentScalingCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      val changes: Seq[(UpstreamAssessmentGroupMember, (Option[Int], Option[Int]), (Option[String], Option[String]), String)] =
        cmd.studentsToSet.map { case (upstreamAssessmentGroupMember, originalMark, originalGrade) =>
          val (scaledMark, scaledGrade) = cmd.scale(originalMark, originalGrade)

          (upstreamAssessmentGroupMember, originalMark -> scaledMark, originalGrade -> scaledGrade, cmd.comment(originalMark))
        }

      model.addAttribute("changes", changes)
      model.addAttribute("returnTo", getReturnTo(Routes.marks.Admin.AssessmentComponents.scaling(assessmentComponent, upstreamAssessmentGroup)))

      "marks/admin/assessment-components/scaling_preview"
    }

  @PostMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: ComponentScalingCommand.Command,
    errors: Errors,
    @PathVariable assessmentComponent: AssessmentComponent,
    @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup,
    model: ModelMap,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()

      RedirectFlashing(Routes.marks.Admin.home(assessmentComponent.module.adminDepartment, upstreamAssessmentGroup.academicYear), "flash__success" -> "flash.assessmentComponent.scaled")
    }

}
