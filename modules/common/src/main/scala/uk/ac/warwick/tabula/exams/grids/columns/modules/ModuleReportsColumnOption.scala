package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns._

import scala.math.BigDecimal.RoundingMode

@Component
class ModuleReportsColumnOption extends ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "modulereports"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.ModuleReports

	case class PassedCoreRequiredColumn(state: ExamGridColumnState)
		extends ExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val category: String = "Modules Report"

		override val title: String = "Passed Required Core Modules?"

		override val renderSecondaryValue: String = ""

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> getValue(entity)).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			cell.setCellValue(getValue(entity))
		}

		private def getValue(entity: GenerateExamGridEntity): String = {
			val coreRequiredModuleRegistrations = entity.moduleRegistrations.filter(mr => state.coreRequiredModules.contains(mr.module))
			if (state.coreRequiredModules.nonEmpty) {
				if (coreRequiredModuleRegistrations.exists(_.agreedGrade == "F")) {
					"N"
				} else {
					"Y"
				}
			} else {
				""
			}
		}

	}

	case class MeanModuleMarkColumn(state: ExamGridColumnState)
		extends ExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val category: String = "Modules Report"

		override val title: String = "Mean Module Mark For This Year"

		override val renderSecondaryValue: String = ""

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> {
				val entityMarks = entity.moduleRegistrations.flatMap(mr => mr.firstDefinedMark).map(mark => BigDecimal(mark))
				if (entityMarks.nonEmpty) {
					(entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString
				} else {
					""
				}
			}).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			val entityMarks = entity.moduleRegistrations.flatMap(mr => mr.firstDefinedMark).map(mark => BigDecimal(mark))
			if (entityMarks.nonEmpty) {
				cell.setCellValue((entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString)
			} else {
				cell.setCellValue("")
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(
		PassedCoreRequiredColumn(state),
		MeanModuleMarkColumn(state)
	)
}
