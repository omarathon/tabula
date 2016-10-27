package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class NameColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "name"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Name

	override val mandatory = true

	private case class FirstNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "First Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(entity.firstName)
			).toMap
		}

	}

	private case class LastNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Last Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(entity.lastName)
			).toMap
		}

	}

	private case class FullNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.LongString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(Seq(entity.firstName, entity.lastName).mkString(" "))
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = state.showFullName match {
		case true => Seq(FullNameColumn(state))
		case false => Seq(FirstNameColumn(state), LastNameColumn(state))
	}

}

@Component
class UniversityIDColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "universityId"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.UniversityId

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "ID"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(entity.universityId)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class StartYearColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "startyear"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.StartYear

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Start Year"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					entity.years.get(state.yearOfStudy).flatMap(_.studentCourseYearDetails).flatMap(
						scyd => Option(scyd.studentCourseDetails.sprStartAcademicYear)
					).map(_.toString).getOrElse("[Unknown]")
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
