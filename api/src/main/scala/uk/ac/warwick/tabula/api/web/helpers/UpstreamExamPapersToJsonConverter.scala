package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.commands.profiles.ExamModuleRegistrationAndComponents
import uk.ac.warwick.tabula.helpers.DurationFormatter

trait UpstreamExamPapersToJsonConverter {

  def jsonUpstreamExamPapersObject(moduleRegAndComponents: ExamModuleRegistrationAndComponents): Map[String, Any] = {
    Map(
      "scjCode" -> moduleRegAndComponents.moduleRegistration._scjCode,
      "module" -> s"${moduleRegAndComponents.moduleRegistration.toSITSCode}",
      "examcomponents" -> moduleRegAndComponents.components.map { component =>
        val uac = component.upstreamGroup.assessmentComponent
        Map("paperCode" -> uac.examPaperCode.get,
          "paperTitle" -> uac.examPaperTitle.get,
          "section" -> uac.examPaperSection,
          "duration" -> uac.examPaperDuration.map { d => DurationFormatter.format(d, false) },
          "readingTime" -> uac.examPaperReadingTime.map { r => DurationFormatter.format(r, false) }.getOrElse("n/a"),
          "examType" -> uac.examPaperType.map(_.name),
          "assessmentType" -> uac.assessmentType.name
        )
      }
    )

  }
}
