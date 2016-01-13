package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class CurrentYearMarkColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val sortOrder: Int = 9

	case class Column(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Mean Module Mark"

		override val category: String = "Year Marks"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> result(entity).map(_.toString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			result(entity).foreach(mark =>
				cell.setCellValue(mark.doubleValue())
			)
		}

		private def result(entity: GenerateExamGridEntity): Option[BigDecimal] = {
			val cats = entity.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum
			if (cats > entity.normalCATLoad && moduleRegistrationService.overcattedModuleSubsets(entity).size > 1 && entity.overcattingModules.isEmpty) {
				// If the student has overcatted, has more than one valid overcat subset, and a subset has not been chosen for the overcatted mark, don't show anything
				None
			} else {
				moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations)
			}
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
