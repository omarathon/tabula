package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringMaintenanceModeServiceComponent

@Component
class PotentialMarkingOptionsColumnOption extends StudentExamGridColumnOption with AutowiringMaintenanceModeServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "markingoptions"

	override val label: String = ""

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PotentialMarkingOptions

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity -> (
				if (maintenanceModeService.enabled)
					ExamGridColumnValueStringHtmlOnly(
						"""<button class="btn btn-default btn-xs use-tooltip" data-placement="right" disabled title="Tabula has been placed in a read-only mode. Managing overcatted marks is not currently possible.">Edit</button>"""
					)
				else
					ExamGridColumnValueStringHtmlOnly(
						"<button class=\"btn btn-default edit-overcatting btn-xs\" type=\"button\" data-student=\"%s\" data-basedonlevel=\"%s\">Edit</button>".format(
							entity.validYears(state.yearOfStudy).studentCourseYearDetails.get.id,
							state.isLevelGrid
						)
					)
				)).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
