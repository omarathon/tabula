package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, ExamGridColumnState}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class PotentialMarkingOptionsColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "markingoptions"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PotentialMarkingOptions

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ExamGridColumn(state) {

		override val title: String = "Potential marking options"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> {
				if (entity.cats > state.normalLoad && state.overcatSubsets(entity).size > 1) {
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

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(Column(state))

}
