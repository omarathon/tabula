package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.helpers.SeqUtils._
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent

import scala.jdk.CollectionConverters._

@Component
class MitigatingCircumstancesColumnOption extends ChosenYearExamGridColumnOption with AutowiringMitCircsSubmissionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "mitigating"

  override val label: String = "Administration: Mitigating Circumstances"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.MitigatingCircumstances

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Mitigating Circumstances"

    override val category: String = "Administration"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.VeryLongString

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {

      state.entities.map(entity => entity -> {
        def globalRecommendations(s: MitigatingCircumstancesSubmission) =  s.globalRecommendations.map(r => s"${r.description} (all assessments)")
        def affectedAssessmentsByRecommendation(s: MitigatingCircumstancesSubmission) = s.affectedAssessmentsByRecommendation.toSeq
          .map{case (r, a) => s"${r.description} ${a.map(_.module.code.toUpperCase).distinct.mkString("(", ", ", ")")}"}
        def modulesWithAcuteOutcomes(s: MitigatingCircumstancesSubmission) = s.assessmentsWithAcuteOutcome.map(_.module.code.toUpperCase).distinct

        val mitCircsCodesString = entity.mitigatingCircumstances.map(s => {
          val header = s"MIT-${s.key} Graded ${s.gradingCode.getOrElse("")} - (approved ${DateFormats.CSVDate.print(s.outcomesFinalisedOn)})\n"
          val global = globalRecommendations(s).mkStringOrEmpty("", "\n", "\n")
          val affected = affectedAssessmentsByRecommendation(s).mkStringOrEmpty("", "\n", "\n")
          val acute = modulesWithAcuteOutcomes(s).mkStringOrEmpty(s"${Option(s.acuteOutcome).map(_.description).getOrElse("")} (", ", ", ")\n")
          header ++ global ++ affected ++ acute
        }).mkString("\n")

        val mitCircsHtml = entity.mitigatingCircumstances.map(s => s"""<dl class="dl-horizontal">
          <dt>MIT-${s.key}<dt><dd>Graded ${s.gradingCode.getOrElse("")} - (approved ${DateFormats.CSVDate.print(s.outcomesFinalisedOn)})<dd>
          <dt>Recommendation</dt>
            ${globalRecommendations(s).mkStringOrEmpty("<dd>", "</dd><dd>", "</dd>")}
            ${affectedAssessmentsByRecommendation(s).mkStringOrEmpty("<dd>", "</dd><dd>", "</dd>")}
            ${modulesWithAcuteOutcomes(s).mkStringOrEmpty(s"<dd>${Option(s.acuteOutcome).map(_.description).getOrElse("")} (", ", ", ")</dd>")}
        </dl>""").mkString

        val notes = if (state.department.rootDepartment.code == "es") {
          entity.validYears.headOption
            .flatMap { case (_, year) => year.studentCourseYearDetails }
            .map(notes => s"\n${notes.studentCourseDetails.notes.asScala.mkString(", ")}")
        } else {
          None
        }

        ExamGridColumnValueStringWithHtml(
          value = s"$mitCircsCodesString${notes.getOrElse("")}",
          html = s"$mitCircsHtml${notes.map(n => s"<span>$n</span>").getOrElse("")}"
        )
      }).toMap
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
