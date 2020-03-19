package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.TopLevelUrlComponent
import uk.ac.warwick.tabula.data.model.AssessmentComponent

trait UpstreamAssessmentsAndExamsToJsonConverter {
  self: TopLevelUrlComponent =>

  def jsonUpstreamAssessmentObject(assessmentComponent: AssessmentComponent): Map[String, Any] = {
    Map(
      "id" -> assessmentComponent.id,
      "cats" -> assessmentComponent.cats,
      "sequence" -> assessmentComponent.sequence,
      "type" -> assessmentComponent.assessmentType,
      "module" -> Map(
        "code" -> assessmentComponent.module.code.toUpperCase,
        "name" -> assessmentComponent.module.name,
        "adminDepartment" -> Map(
          "code" -> assessmentComponent.module.adminDepartment.code.toUpperCase,
          "name" -> assessmentComponent.module.adminDepartment.name
        )
      ),
      "examPaper" -> (assessmentComponent.examPaperCode match {
        case Some(examPaperCode) => Map(
          "code" -> examPaperCode,
          "duration" -> assessmentComponent.examPaperDuration,
          "title" -> assessmentComponent.examPaperTitle,
          "readingTime" -> assessmentComponent.examPaperReadingTime,
          "section" -> assessmentComponent.examPaperSection,
          "type" -> assessmentComponent.examPaperType
        )
        case _ => null
      })
)}

}
