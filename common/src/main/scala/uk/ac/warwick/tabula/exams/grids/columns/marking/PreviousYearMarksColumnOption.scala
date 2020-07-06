package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, AutowiringProgressionServiceComponent}

@Component
class PreviousYearMarksColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringProgressionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "previous"

  override val label: String = "Marking: Marks from previous year(s)"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.PreviousYears

  case class Column(state: ExamGridColumnState, thisYearOfStudy: Int) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = s"Year $thisYearOfStudy"

    override val category: String = "Previous Year Marks"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> (markOrError(entity) match {
          case Right(mark) => ExamGridColumnValueDecimal(mark)
          case Left(error) => ExamGridColumnValueMissing(error)
        })
      ).toMap
    }

    private def markOrError(entity: ExamGridEntity): Either[String, BigDecimal] = {
      entity.years.filter { case (_, entityYear) => entityYear.nonEmpty }.get(state.yearOfStudy).flatten match {
        case Some(year) if year.studentCourseYearDetails.nonEmpty =>
          val marksPerYear =
            progressionService.marksPerYear(
              year.studentCourseYearDetails.get,
              state.normalLoadLookup(year.route),
              Map(thisYearOfStudy -> state.routeRulesLookup(year.route, year.level)),
              state.yearMarksToUse,
              state.isLevelGrid,
              entity.yearWeightings,
              markForFinalYear = false
            )

          marksPerYear.flatMap(_.get(thisYearOfStudy).toRight(s"No year mark for Year ${year.yearOfStudy}"))

        case _ => Left(s"No course detail found for ${entity.universityId} for Year $thisYearOfStudy")
      }
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = {
    val requiredYears = 1 until state.yearOfStudy
    requiredYears.map(year => Column(state, year))
  }

}
