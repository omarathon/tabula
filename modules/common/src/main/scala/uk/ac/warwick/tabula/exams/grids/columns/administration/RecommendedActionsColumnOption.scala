package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class RecommendedActionsColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "recommendedactions"

	override val sortOrder: Int = 12

	case class Column(entities: Seq[GenerateExamGridEntity])
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = "Recommended Actions"

		override val category: String = "Administration"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> {
				val hasOvercatted = entity.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum > entity.normalCATLoad
				val hasMoreThanOneSubset = moduleRegistrationService.overcattedModuleSubsets(entity).size > 1
				if (hasOvercatted && hasMoreThanOneSubset) {
					"<button class=\"btn btn-default edit-overcatting\" type=\"button\" data-student=\"%s\">Edit</button>".format(entity.id)
				} else {
					// TODO: Populate from SITS
					""
				}
			}).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
			// TODO: Populate from SITS
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
