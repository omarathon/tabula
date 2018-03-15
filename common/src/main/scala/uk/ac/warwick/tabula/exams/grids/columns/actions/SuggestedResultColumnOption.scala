package uk.ac.warwick.tabula.exams.grids.columns.actions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringProgressionServiceComponent, ProgressionResult}

@Component
class SuggestedResultColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "suggestedresult"

	override val label: String = "Suggested Actions: Suggested result"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.SuggestedResult

	case class Column(state: ExamGridColumnState)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Suggested Result"

		override val category: String = "Suggested Actions"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.filter { case (_, entityYear) => entityYear.nonEmpty }.get(state.yearOfStudy).map(entityYear =>
					progressionService.suggestedResult(
						entityYear.get.studentCourseYearDetails.get,
						state.normalLoadLookup(entityYear.get.route),
						entity.validYears.mapValues(ey => state.routeRulesLookup(ey.route, ey.level)),
						state.calculateYearMarks
					) match {
						case unknown: ProgressionResult.Unknown => ExamGridColumnValueMissing(unknown.details)
						case result => ExamGridColumnValueString(result.description)
					}
				).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
