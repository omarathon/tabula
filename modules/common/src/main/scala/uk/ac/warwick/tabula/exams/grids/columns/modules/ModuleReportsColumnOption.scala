package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridExporter, GenerateExamGridEntity}
import uk.ac.warwick.tabula.data.model.{ModuleSelectionStatus, Module}
import uk.ac.warwick.tabula.exams.grids.columns.{HasExamGridColumnSecondaryValue, HasExamGridColumnCategory, ExamGridColumn, ExamGridColumnOption}

import scala.math.BigDecimal.RoundingMode

@Component
class ModuleReportsColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "modulereports"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.ModuleReports

	case class PassedCoreRequiredColumn(entities: Seq[GenerateExamGridEntity], departmentCoreRequiredModules: Seq[Module])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val category: String = "Module reports"

		override val title: String = "Passed required core modules"

		override val renderSecondaryValue: String = ""

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> getValue(entity)).toMap

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
			val coreRequiredModules = entity.moduleRegistrations.filter(mr => mr.selectionStatus == ModuleSelectionStatus.CoreRequired || departmentCoreRequiredModules.contains(mr.module))
			if (coreRequiredModules.nonEmpty) {
				if (coreRequiredModules.exists(_.agreedGrade == "F")) {
					"N"
				} else {
					"Y"
				}
			} else {
				""
			}
		}

	}

	case class MeanModuleMarkColumn(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val category: String = "Module reports"

		override val title: String = "Mean module mark for this year"

		override val renderSecondaryValue: String = ""

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> {
				val entityMarks = entity.moduleRegistrations.flatMap(mr => Option(mr.agreedMark)).map(mark => BigDecimal(mark))
				(entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString
			}).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			val entityMarks = entity.moduleRegistrations.flatMap(mr => Option(mr.agreedMark)).map(mark => BigDecimal(mark))
			cell.setCellValue((entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).doubleValue)
		}

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(
		PassedCoreRequiredColumn(entities, departmentCoreRequiredModules),
		MeanModuleMarkColumn(entities)
	)
}
