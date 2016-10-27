package uk.ac.warwick.tabula.exams.grids.columns

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, UpstreamRouteRule}

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
		val TotalCATS = 20
		val PassedCATS = 21
		val PreviousYears = 22
		val CurrentYear = 23
		val OvercattedYearMark = 24
		val BoardAgreedMark = 25
		val SuggestedResult = 30
		val SuggestedFinalYearGrade = 31
		val MitigatingCircumstances = 40
		val Comments = 50
	}

	object ExcelColumnSizes {
		val Spacer = 600
		val WholeMark = 900
		val Decimal = 1200
		val ShortString = 1700
		val LongString = 4000
	}
}

case class ExamGridColumnState(
	entities: Seq[ExamGridEntity],
	overcatSubsets: Map[ExamGridEntityYear, Seq[(BigDecimal, Seq[ModuleRegistration])]],
	coreRequiredModules: Seq[Module],
	normalLoad: BigDecimal,
	routeRules: Seq[UpstreamRouteRule],
	academicYear: AcademicYear,
	yearOfStudy: Int,
	showFullName: Boolean,
	showComponentMarks: Boolean
)

case object EmptyExamGridColumnState {
	def apply() = ExamGridColumnState(Nil,Map.empty,Nil,0,Nil,null,0,showFullName=true,showComponentMarks=false)
}

@Component
sealed trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val sortOrder: Int
	val mandatory: Boolean = false

}

trait StudentExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn]
}

trait PerYearExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]]
}

trait ChosenYearExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn]
}

sealed abstract class ExamGridColumn(state: ExamGridColumnState) {
	val title: String
	val excelColumnWidth: Int
}

abstract class PerYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {
	def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]
}

abstract class ChosenYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {
	def values: Map[ExamGridEntity, ExamGridColumnValue]
}


trait HasExamGridColumnCategory {

	self: ExamGridColumn =>

	val category: String

}

trait HasExamGridColumnSecondaryValue {

	self: ExamGridColumn =>

	val secondaryValue: String

}