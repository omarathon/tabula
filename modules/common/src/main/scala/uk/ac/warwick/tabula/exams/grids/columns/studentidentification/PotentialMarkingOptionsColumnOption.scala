package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class PotentialMarkingOptionsColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "markingoptions"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PotentialMarkingOptions

	override val mandatory = true

	case class Column(entities: Seq[GenerateExamGridEntity]) extends ExamGridColumn(entities) {

		override val title: String = "Potential marking options"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> {
				val hasOvercatted = entity.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum > entity.normalCATLoad
				val hasMoreThanOneSubset = moduleRegistrationService.overcattedModuleSubsets(entity, entity.markOverrides.getOrElse(Map())).size > 1
				if (hasOvercatted && hasMoreThanOneSubset) {
					"<button class=\"btn btn-default edit-overcatting\" type=\"button\" data-student=\"%s\">Edit</button>".format(entity.id)
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
			row.createCell(index)
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = Seq(Column(entities))

}
