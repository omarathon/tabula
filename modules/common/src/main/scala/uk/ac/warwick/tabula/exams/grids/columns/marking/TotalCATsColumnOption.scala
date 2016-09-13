package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class TotalCATsColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "cats"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.TotalCATs

	case class Column(state: ExamGridColumnState, bound: BigDecimal, isUpperBound: Boolean = false, isTotal: Boolean = false)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = if (isTotal) "Total Cats" else if (isUpperBound) s"<=$bound" else s">=$bound"

		override val category: String = "CATS"

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entityYear => result(entityYear) match {
					case Right(mark) => ExamGridColumnValueDecimal(mark)
					case Left(message) => ExamGridColumnValueMissing(message)
				}).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

		private def result(entity: ExamGridEntityYear): Either[String, JBigDecimal] = {
			def transformModuleRegistrations(moduleRegistrations: Seq[ModuleRegistration]): JBigDecimal = {
				moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying
			}

			if (!isTotal && entity.moduleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
				Left(s"The total CATS cannot be calculated because the following module registrations have no mark: ${entity.moduleRegistrations.filter(_.firstDefinedMark.isEmpty).map(_.module.code).mkString(", ")}")
			} else if (isTotal) {
				Right(transformModuleRegistrations(entity.moduleRegistrations))
			} else if (isUpperBound) {
				Right(transformModuleRegistrations(entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) <= bound))))
			} else {
				val f = entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) >= bound))
				val f2 = f.map(mr => BigDecimal(mr.cats))
				val f3 = f.map(mr => BigDecimal(mr.cats)).sum
				val f4 = f.map(mr => BigDecimal(mr.cats)).sum.underlying
				val f1 = transformModuleRegistrations(f)
				Right(f1)
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] =
		Seq(
			Column(state, BigDecimal(30), isUpperBound = true),
			Column(state, BigDecimal(40)),
			Column(state, BigDecimal(50)),
			Column(state, BigDecimal(60)),
			Column(state, BigDecimal(70)),
			Column(state, BigDecimal(0), isTotal = true)
		)

}
