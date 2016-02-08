package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory, HasExamGridColumnSecondaryValue}

import scala.math.BigDecimal.RoundingMode

@Component
class ModuleReportsColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "modulereports"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.ModuleReports

	case class PassedCoreRequiredColumn(entities: Seq[GenerateExamGridEntity], coreRequiredModules: Seq[Module])
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
			val coreRequiredModuleRegistrations = entity.moduleRegistrations.filter(mr => coreRequiredModules.contains(mr.module))
			if (coreRequiredModules.nonEmpty) {
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

	case class MeanModuleMarkColumn(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val category: String = "Module reports"

		override val title: String = "Mean module mark for this year"

		override val renderSecondaryValue: String = ""

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> {
				val entityMarks = entity.moduleRegistrations.flatMap(mr => Option(mr.agreedMark)).map(mark => BigDecimal(mark))
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
			val entityMarks = entity.moduleRegistrations.flatMap(mr => Option(mr.agreedMark)).map(mark => BigDecimal(mark))
			if (entityMarks.nonEmpty) {
				cell.setCellValue((entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString)
			} else {
				cell.setCellValue("")
			}
		}

	}

	override def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(
		PassedCoreRequiredColumn(entities, coreRequiredModules),
		MeanModuleMarkColumn(entities)
	)
}
