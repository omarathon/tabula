package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.TopLevelUrlComponent
import uk.ac.warwick.tabula.commands.exams.grids.ModuleRegistrationAndComponents

trait StudentUpstreamExamPapersToJsonConverter extends UpstreamAssessmentsAndExamsToJsonConverter {
  self: TopLevelUrlComponent =>
  def jsonUpstreamExamPapersObject(moduleRegAndComponents: ModuleRegistrationAndComponents): Map[String, Any] = {
    Map(
      "sprCode" -> moduleRegAndComponents.moduleRegistration.sprCode,
      "moduleCode" -> s"${moduleRegAndComponents.moduleRegistration.sitsModuleCode}",
      "examComponents" -> moduleRegAndComponents.components.map { component =>
        val ac = component.upstreamGroup.assessmentComponent
        val academicYear = moduleRegAndComponents.moduleRegistration.academicYear
        val scheduledExams = ac.scheduledExams(Some(academicYear))
        Map(
          jsonExamPaperObject(ac, scheduledExams),
          "type" -> ac.assessmentType
        )
      }
    )

  }
}
