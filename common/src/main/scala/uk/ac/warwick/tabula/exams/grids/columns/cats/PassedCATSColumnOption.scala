package uk.ac.warwick.tabula.exams.grids.columns.cats

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.helpers.StringUtils._

@Component
class PassedCATSColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "passedCats"

	override val label: String = "CATS breakdowns: Passed CATS"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PassedCATS

	case class Column(state: ExamGridColumnState, thisYearOfStudy: Int)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = s"Year $thisYearOfStudy"

		override val category: String = "Passed CATS"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> relevantEntityYear(entity).map(entityYear => result(entityYear))
					.getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for year $thisYearOfStudy"))
			).toMap
		}

		private def result(entity: ExamGridEntityYear): ExamGridColumnValue = {

			val emptyExpectingMarks = entity.moduleRegistrations.filter(mr => !mr.passFail && mr.firstDefinedMark.isEmpty)
			val emptyExpectingGrades = entity.moduleRegistrations.filter(mr => mr.passFail && mr.firstDefinedGrade.isEmpty)

			if (emptyExpectingMarks.nonEmpty || emptyExpectingGrades.nonEmpty) {

				val noMarks = if(emptyExpectingMarks.isEmpty) "" else emptyExpectingMarks.map(_.module.code.toUpperCase).mkString("the following module registrations have no mark: ", ", ", "")
				val noGrades = if(emptyExpectingGrades.isEmpty) "" else emptyExpectingGrades.map(_.module.code.toUpperCase).mkString("the following module registrations have no grade: ", ", ", "")

				val reasons = Seq(noMarks, noGrades).filter(_.hasText)
				ExamGridColumnValueMissing("The passed CATS cannot be calculated because %s".format(reasons.mkString(" and ")))
			} else {
				def isFailed(mr: ModuleRegistration): Boolean = {
					if(mr.passFail) !mr.firstDefinedGrade.contains("F")
					else if (Option(mr.agreedMark).isDefined) mr.agreedGrade != "F"
					else mr.actualGrade != "F"
				}

				ExamGridColumnValueDecimal(entity.moduleRegistrations.filter(isFailed).map(mr => BigDecimal(mr.cats)).sum.underlying)
			}
		}

		/**
			* Gets the ExamGridEntityYear for this previous year of study.
			* This may have already been calculated if we're showing previous year registrations.
			* If not we need to re-fetch it.
			*/
		private def relevantEntityYear(entity: ExamGridEntity): Option[ExamGridEntityYear] = {
			entity.validYears.get(thisYearOfStudy).orElse(
				entity.validYears.values.lastOption.flatMap(entityYear =>
					// For the last year go back up to the student and re-fetch the ExamGridEntity
					entityYear.studentCourseYearDetails.get.studentCourseDetails.student.toExamGridEntity(entityYear.studentCourseYearDetails.get)
						// Then see if a matching ExamGrdEntityYear exists
						.validYears.get(thisYearOfStudy)
				)
			)
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] =	{
		val requiredYears = 1 to state.yearOfStudy
		requiredYears.map(year => Column(state, year))
	}

}
