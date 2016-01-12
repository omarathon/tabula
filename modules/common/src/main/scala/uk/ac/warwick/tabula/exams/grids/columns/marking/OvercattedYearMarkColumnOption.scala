package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class OvercattedYearMarkColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "overcatted"

	override val sortOrder: Int = 9

	case class Column(scyds: Seq[StudentCourseYearDetails])
		extends ExamGridColumn(scyds) with HasExamGridColumnCategory {

		override val title: String = "Over Catted Mark"

		override val category: String = "Year Marks"

		override def render: Map[String, String] =
			scyds.map(scyd => scyd.id -> result(scyd).map(_.toString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			scyd: StudentCourseYearDetails,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			result(scyd).foreach(mark =>
				cell.setCellValue(mark.doubleValue())
			)
		}

		private def result(scyd: StudentCourseYearDetails): Option[BigDecimal] = {
			val cats = scyd.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum
			if (cats > scyd.normalCATLoad && scyd.overcattingModules.isDefined) {
				// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
				// calculate the overcatted mark from that subset
				moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations.filter(mr => scyd.overcattingModules.get.contains(mr.module)))
			} else {
				None
			}
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = Seq(Column(scyds))

}
