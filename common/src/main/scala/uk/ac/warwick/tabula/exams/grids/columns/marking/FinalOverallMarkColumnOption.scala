package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringProgressionServiceComponent, FinalYearGrade, FinalYearMark}

@Component
class FinalOverallMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "finalOverallMark"

  override val label: String = "Marking: Final overall mark"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.FinalOverallMark

  case class Column(state: ExamGridColumnState)
    extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Final Overall Mark"

    override val category: String = "Marking"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear =>
          progressionService.suggestedFinalYearGrade(
            entityYear,
            state.normalLoadLookup(entityYear),
            entity.validYears.view.mapValues(ey => state.routeRulesLookup(ey)).toMap,
            state.yearMarksToUse,
            state.isLevelGrid,
            state.applyBenchmark,
            entity.yearWeightings
          ) match {
            case unknown: FinalYearGrade.Unknown => ExamGridColumnValueMissing(unknown.reason)
            case withMark: FinalYearMark => ExamGridColumnValueDecimal(withMark.mark)
            case result if result.details.nonEmpty => ExamGridColumnValueWithTooltip(result.description, message = result.details.get)
            case result => ExamGridColumnValueString(result.description)
          }
        ).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
