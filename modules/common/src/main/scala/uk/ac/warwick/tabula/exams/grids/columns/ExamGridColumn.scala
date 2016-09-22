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
	entities: Seq[ExamGridEntity],
	overcatSubsets: Map[ExamGridEntityYear, Seq[(BigDecimal, Seq[ModuleRegistration])]],
	coreRequiredModules: Seq[Module],
	normalLoad: BigDecimal,
	routeRules: Seq[UpstreamRouteRule],
	academicYear: AcademicYear,
	yearOfStudy: Int
)

case object EmptyExamGridColumnState {
	def apply() = ExamGridColumnState(Nil,Map.empty,Nil,0,Nil,null,0)
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
}

abstract class PerYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {
	def values: Map[ExamGridEntity, Map[YearOfStudy, ExamGridColumnValue]]
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