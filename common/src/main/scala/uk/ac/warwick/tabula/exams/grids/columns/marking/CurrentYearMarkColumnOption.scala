package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.CourseYearWeighting
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringModuleRegistrationServiceComponent}

@Component
class CurrentYearMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringCourseAndRouteServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val label: String = "Marking: Current year weighted mean mark"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CurrentYear

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Weighted mean year mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => result(entityYear, entity) match {
					case Right(mark) => ExamGridColumnValueDecimal(mark)
					case Left(message) => ExamGridColumnValueMissing(message)
				}).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

		private def result(entityYear: ExamGridEntityYear, entity: ExamGridEntity): Either[String, BigDecimal] = {
			if (state.overcatSubsets(entityYear).size > 1 && entityYear.overcattingModules.isEmpty) {
				// If there is more than one valid overcat subset, and a subset has not been chosen for the overcatted mark, don't show anything
				Left("The overcat adjusted mark subset has not been chosen")
			} else {
				moduleRegistrationService.weightedMeanYearMark(entityYear.moduleRegistrations, entityYear.markOverrides.getOrElse(Map()), allowEmpty = entity.yearWeightings.exists( w => w.yearOfStudy == entityYear.yearOfStudy && w.weighting == 0))
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
