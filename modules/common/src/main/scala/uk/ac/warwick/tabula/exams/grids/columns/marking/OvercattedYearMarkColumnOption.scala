package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, ExamGridColumnState, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class OvercattedYearMarkColumnOption extends ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "overcatted"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.OvercattedYearMark

	override val mandatory = true

	case class Column(state: ExamGridColumnState)	extends ExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Over Catted Mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> result(entity).map(_.toString).getOrElse("")).toMap

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
			// If the entity isn't based on an SCYD i.e. when we're showing the overcatting options, just show the overcat mark for this subset
			if (entity.studentCourseYearDetails.isEmpty) {
				moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations, entity.markOverrides.getOrElse(Map()))
			} else {
				if (entity.cats > state.normalLoad) {
					val overcatSubsets = state.overcatSubsets(entity)
					if (overcatSubsets.size == 1) {
						// If the student has overcatted, but there's only one valid subset, just show the mark for that subset
						Option(overcatSubsets.head._1)
					} else if (entity.overcattingModules.isDefined) {
						// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
						// calculate the overcatted mark from that subset
						moduleRegistrationService.weightedMeanYearMark(
							entity.moduleRegistrations.filter(mr => entity.overcattingModules.get.contains(mr.module)),
							entity.markOverrides.getOrElse(Map())
						)
					} else {
						None
					}
				} else {
					None
				}
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(Column(state))

}
