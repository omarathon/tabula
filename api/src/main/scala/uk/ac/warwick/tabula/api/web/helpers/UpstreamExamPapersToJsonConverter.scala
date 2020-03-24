package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.commands.exams.grids.ModuleRegistrationAndComponents

trait UpstreamExamPapersToJsonConverter {

  def jsonUpstreamExamPapersObject(moduleRegAndComponents: ModuleRegistrationAndComponents): Map[String, Any] = {
    Map(
      "scjCode" -> moduleRegAndComponents.moduleRegistration._scjCode,
      "moduleCode" -> s"${moduleRegAndComponents.moduleRegistration.toSITSCode}",
      "examComponents" -> moduleRegAndComponents.components.map { component =>
        val uac = component.upstreamGroup.assessmentComponent
        Map(
          "examPaper" -> Map(
            "code" -> uac.examPaperCode.get,
            "duration" -> uac.examPaperDuration.toString,
            "title" -> uac.examPaperTitle.get,
            "readingTime" -> uac.examPaperReadingTime,
            "section" -> uac.examPaperSection,
            "type" -> uac.examPaperType
          ),
          "type" -> uac.assessmentType
        )
      }
    )

  }
}
