package uk.ac.warwick.tabula.exams.grids.columns

import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity

object BlankColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "blank"

	override val sortOrder: Int = Int.MaxValue

	case class Column(state: ExamGridColumnState, override val title: String)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val category: String = "Additional"

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity -> ExamGridColumnValueString("")).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = throw new UnsupportedOperationException

	def getColumn(title: String): Seq[ChosenYearExamGridColumn] = Seq(Column(EmptyExamGridColumnState(), title))

}
