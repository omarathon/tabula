package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnState, ExamGridColumn, ExamGridColumnOption}

@Component
class NameColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "name"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Name

	case class Column(state: ExamGridColumnState) extends ExamGridColumn(state) {

		override val title: String = "Name"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> entity.name).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			cell.setCellValue(entity.name)
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(Column(state))

}

@Component
class UniversityIDColumnOption extends ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "universityId"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.UniversityId

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ExamGridColumn(state) {

		override val title: String = "ID"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> entity.universityId).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			cell.setCellValue(entity.universityId)
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(Column(state))

}

@Component
class StartYearColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "startyear"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.StartYear

	case class Column(state: ExamGridColumnState) extends ExamGridColumn(state) {

		override val title: String = "Start Year"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> entity.studentCourseYearDetails.flatMap(scyd =>
				Option(scyd.studentCourseDetails.sprStartAcademicYear).map(_.toString)
			).getOrElse("[Unknown]")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			cell.setCellValue(entity.studentCourseYearDetails.flatMap(scyd =>
				Option(scyd.studentCourseDetails.sprStartAcademicYear).map(_.toString)
			).getOrElse("[Unknown]"))
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = Seq(Column(state))

}
