package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption}

@Component
class NameColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "name"

	override val sortOrder: Int = 1

	case class Column(scyds: Seq[StudentCourseYearDetails]) extends ExamGridColumn(scyds) {

		override val title: String = "Name"

		override def render: Map[String, String] =
			scyds.map(scyd => scyd.id -> scyd.studentCourseDetails.student.fullName.getOrElse("[Unknown]")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			scyd: StudentCourseYearDetails,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = createCell(row)
			cell.setCellValue(scyd.studentCourseDetails.student.fullName.getOrElse("[Unknown]"))
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = Seq(Column(scyds))

}

@Component
class UniversityIDColumnOption extends ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "universityId"

	override val sortOrder: Int = 2

	case class Column(scyds: Seq[StudentCourseYearDetails]) extends ExamGridColumn(scyds) {

		override val title: String = "ID"

		override def render: Map[String, String] =
			scyds.map(scyd => scyd.id -> scyd.studentCourseDetails.student.universityId).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			scyd: StudentCourseYearDetails,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = createCell(row)
			cell.setCellValue(scyd.studentCourseDetails.student.universityId)
		}

	}

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = Seq(Column(scyds))

}
