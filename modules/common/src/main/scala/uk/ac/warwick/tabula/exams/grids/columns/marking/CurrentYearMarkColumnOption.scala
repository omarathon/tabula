package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class CurrentYearMarkColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val sortOrder: Int = 9

	case class Column(scyds: Seq[StudentCourseYearDetails])
		extends ExamGridColumn(scyds) with HasExamGridColumnCategory {

		override val title: String = "Mean Module Mark"

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
			if (cats > scyd.normalCATLoad) {
				// TODO: Check is an overcat mark has been calculated yet
				None
			} else {
				moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations)
			}
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = Seq(Column(scyds))

}
