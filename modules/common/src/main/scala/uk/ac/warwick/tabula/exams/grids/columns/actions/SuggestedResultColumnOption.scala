package uk.ac.warwick.tabula.exams.grids.columns.actions

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.{ProgressionResult, AutowiringProgressionServiceComponent}

@Component
class SuggestedResultColumnOption extends columns.ExamGridColumnOption with AutowiringProgressionServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "suggestedresult"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.SuggestedResult

	case class Column(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Suggested Result"

		override val category: String = "Suggested Actions"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> {
				progressionService.suggestedResult(entity.studentCourseYearDetails.get) match {
					case unknown: ProgressionResult.Unknown => "<span title=\"%s\">%s</span>".format(unknown.details, unknown.description)
					case result => result.description
				}
			}).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			progressionService.suggestedResult(entity.studentCourseYearDetails.get) match {
				case unknown: ProgressionResult.Unknown => cell.setCellValue(s"${unknown.description} ${unknown.details}")
				case result => cell.setCellValue(result.description)
			}
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
