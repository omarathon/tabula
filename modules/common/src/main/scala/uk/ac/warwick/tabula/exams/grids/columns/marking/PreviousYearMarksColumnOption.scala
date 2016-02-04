package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class PreviousYearMarksColumnOption extends YearColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "previous"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PreviousYears

	override val mandatory = true

	case class Column(entities: Seq[GenerateExamGridEntity], yearOfStudy: Int) extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val title: String = s"Year $yearOfStudy"

		override val category: String = "Previous Year Marks"

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
			val scydsFromThisAndOlderCourses: Seq[StudentCourseYearDetails] = entity.studentCourseYearDetails.map(scyd => {
				val scds = scyd.studentCourseDetails.student.freshStudentCourseDetails.sorted.takeWhile(_.scjCode != scyd.studentCourseDetails.scjCode) ++ Seq(scyd.studentCourseDetails)
				scds.flatMap(_.freshStudentCourseYearDetails)
			}).getOrElse(Seq())
			val scydsForThisYear = scydsFromThisAndOlderCourses.filter(scyd => scyd.yearOfStudy.toInt == yearOfStudy)
			val latestSCYDForThisYear = scydsForThisYear.lastOption // SCDs and SCYDs are sorted collections
			latestSCYDForThisYear.flatMap(scyd => Option(scyd.agreedMark).map(mark => BigDecimal(mark)))
		}

	}

	override def getColumns(yearOfStudy: Int, entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =	{
		val requiredYears = 1 until yearOfStudy
		requiredYears.map(year => Column(entities, year))
	}

}
