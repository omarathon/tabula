package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import scala.collection.JavaConverters._

@Component
class MitigatingCircumstancesColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "mitigating"

	override val label: String = "Administration: Mitigating Circumstances"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.MitigatingCircumstances

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Mitigating Circumstances"

		override val category: String = "Administration"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			if(state.department.code == "es" || Option(state.department.parent).exists(_.code == "es")){
				state.entities.map(entity => {
					val notes = entity.validYears.headOption
						.flatMap{case (_, year) => year.studentCourseYearDetails}
						.map(_.studentCourseDetails.notes.asScala.mkString(", "))
					  .getOrElse("")
					entity -> ExamGridColumnValueString(notes)
				}).toMap
			} else {
				state.entities.map(entity => entity -> ExamGridColumnValueString("")).toMap
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
