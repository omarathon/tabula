package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent

import scala.jdk.CollectionConverters._

@Component
class MitigatingCircumstancesColumnOption extends ChosenYearExamGridColumnOption with AutowiringMitCircsSubmissionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "mitigating"

  override val label: String = "Administration: Mitigating Circumstances"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.MitigatingCircumstances

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Mitigating Circumstances"

    override val category: String = "Administration"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      val students = state.entities.flatMap(
        _.validYears.headOption
          .flatMap { case (_, year) => year.studentCourseYearDetails }
          .map(_.studentCourseDetails.student)
      )

      val gradeCodes = mitCircsSubmissionService.mitigationGradesForStudents(students)

      state.entities.map(entity => entity -> {
        val mitCircsCodes = gradeCodes.getOrElse(entity.universityId, Nil)
          .map(gc => s"${gc.code} - (${DateFormats.CSVDate.print(gc.date)})")
          .mkString(", ")

        val notes = if (state.department.rootDepartment.code == "es") {
          entity.validYears.headOption
            .flatMap { case (_, year) => year.studentCourseYearDetails }
            .map(notes => s"\n${notes.studentCourseDetails.notes.asScala.mkString(", ")}")
            .getOrElse("")
        } else {
          ""
        }

        ExamGridColumnValueString(s"$mitCircsCodes$notes")
      }).toMap
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
