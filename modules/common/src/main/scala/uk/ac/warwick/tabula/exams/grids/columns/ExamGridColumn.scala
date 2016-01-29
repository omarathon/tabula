package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.exams.grids.columns

object ExamGridColumnOption {
	type Identifier = String
	implicit val defaultOrdering = Ordering.by { columnOption: ExamGridColumnOption => columnOption.sortOrder }

	object SortOrders {
		val Name = 1
		val UniversityId = 2
		val StartYear = 3
		val CoreModules = 4
		val CoreRequiredModules = 5
		val CoreOptionalModules = 6
		val OptionalModules = 7
		val TotalCATs = 8
		val PreviousYears = 9
		val CurrentYear = 10
		val OvercattedYearMark = 11
		val MitigatingCircumstances = 12
		val RecommendedActions = 13
		val Comments = 20
	}
}

@Component
trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val sortOrder: Int
	val mandatory: Boolean = false
	def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn]

}

abstract class ExamGridColumn(entities: Seq[GenerateExamGridEntity]) {

	val title: String
	def render: Map[String, String]
	def renderExcelCell(
		row: XSSFRow,
		index: Int,
		entity: GenerateExamGridEntity,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
	): Unit

}

trait HasExamGridColumnCategory {

	self: ExamGridColumn =>

	val category: String

}

trait HasExamGridColumnSecondaryValue {

	self: ExamGridColumn =>

	val renderSecondaryValue: String

}

trait HasExamGridColumnSection {

	self: ExamGridColumn =>

	val sectionIdentifier: String

	val sectionTitleLabel: String

	val sectionSecondaryValueLabel: String

	val sectionValueLabel: String
}

object BlankColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "blank"

	override val sortOrder: Int = Int.MaxValue

	case class Column(entities: Seq[GenerateExamGridEntity], override val title: String)
		extends ExamGridColumn(entities) with HasExamGridColumnCategory {

		override val category: String = "Additional"

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> "").toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumn(title: String): Seq[ExamGridColumn] = Seq(Column(Nil, title))

}
