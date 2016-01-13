package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class OvercattedYearMarkColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "overcatted"

	override val sortOrder: Int = 10

	case class Column(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Over Catted Mark"

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
			if (cats > entity.normalCATLoad) {
				if (moduleRegistrationService.overcattedModuleSubsets(entity).size <= 1) {
					// If the student has overcatted, but there's only one valid subset, just show the mean mark
					moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations)
				} else if (entity.overcattingModules.isDefined) {
					// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
					// calculate the overcatted mark from that subset
					moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations.filter(mr => entity.overcattingModules.get.contains(mr.module)))
				} else {
					None
				}
			} else {
				None
			}
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
