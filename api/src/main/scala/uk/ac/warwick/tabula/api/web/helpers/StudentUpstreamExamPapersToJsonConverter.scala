package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.TopLevelUrlComponent
import uk.ac.warwick.tabula.commands.exams.grids.ModuleRegistrationAndComponents

trait StudentUpstreamExamPapersToJsonConverter extends UpstreamAssessmentsAndExamsToJsonConverter {
  self: TopLevelUrlComponent =>
  def jsonUpstreamExamPapersObject(moduleRegAndComponents: ModuleRegistrationAndComponents): Map[String, Any] = {
    Map(
      "scjCode" -> moduleRegAndComponents.moduleRegistration._scjCode,
      "moduleCode" -> s"${moduleRegAndComponents.moduleRegistration.toSITSCode}",
      "examComponents" -> moduleRegAndComponents.components.map { component =>
        val ac = component.upstreamGroup.assessmentComponent
        Map(
          jsonExamPaperObject(ac),
          "type" -> ac.assessmentType
        )
      }
    )

  }
}
