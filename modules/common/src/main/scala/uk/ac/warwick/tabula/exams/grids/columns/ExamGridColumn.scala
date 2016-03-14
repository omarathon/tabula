package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, UpstreamRouteRule, Module}
import uk.ac.warwick.tabula.exams.grids.columns

object ExamGridColumnOption {
	type Identifier = String
	implicit val defaultOrdering = Ordering.by { columnOption: ExamGridColumnOption => columnOption.sortOrder }

	object SortOrders {
		val PotentialMarkingOptions = 0
		val Name = 1
		val UniversityId = 2
		val StartYear = 3
		val CoreModules = 10
		val CoreRequiredModules = 11
		val CoreOptionalModules = 12
		val OptionalModules = 13
		val ModuleReports = 14
		val TotalCATs = 20
		val PreviousYears = 21
		val CurrentYear = 22
		val OvercattedYearMark = 23
		val BoardAgreedMark = 24
		val SuggestedResult = 30
		val SuggestedFinalYearGrade = 31
		val MitigatingCircumstances = 40
		val Comments = 50
	}
}

case class ExamGridColumnState(
	entities: Seq[GenerateExamGridEntity],
	overcatSubsets: Map[GenerateExamGridEntity, Seq[(BigDecimal, Seq[ModuleRegistration])]],
	coreRequiredModules: Seq[Module],
	normalLoad: BigDecimal,
	routeRules: Seq[UpstreamRouteRule],
	yearOfStudy: Int
)

case object EmptyExamGridColumnState {
	def apply() = ExamGridColumnState(Nil,Map.empty,Nil,0,Nil,0)
}

@Component
trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val sortOrder: Int
	val mandatory: Boolean = false
	def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn]

}

abstract class ExamGridColumn(state: ExamGridColumnState) {

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

	case class Column(state: ExamGridColumnState, override val title: String)
		extends ExamGridColumn(state) with HasExamGridColumnCategory {

		override val category: String = "Additional"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> "").toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumn(title: String): Seq[ExamGridColumn] = Seq(Column(EmptyExamGridColumnState(), title))

}
