package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnValueString, _}
import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode

abstract class ModuleReportsColumn(state: ExamGridColumnState) extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue

@Component
class ModuleReportsColumnOption extends PerYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "modulereports"

	override val label: String = "Modules: Module Reports"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.ModuleReports

	case class PassedCoreRequiredColumn(state: ExamGridColumnState)
		extends ModuleReportsColumn(state) {

		override val category: String = "Modules Report"

		override val title: String = "Passed Required Core Modules?"

		override val secondaryValue: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

		private lazy val _values = mutable.Map[ExamGridEntityYear, ExamGridColumnValues]()

		def result(entity: ExamGridEntityYear): ExamGridColumnValues = _values.get(entity) match {
			case Some(values) => values
			case _ =>
				val coreRequiredModules = state.coreRequiredModuleLookup(entity.route).map(_.module)
				val coreRequiredModuleRegistrations = entity.moduleRegistrations.filter(mr => coreRequiredModules.contains(mr.module))
				val values = if (coreRequiredModules.nonEmpty) {
					if (coreRequiredModuleRegistrations.exists(_.agreedGrade == "F")) {
						ExamGridColumnValues(ExamGridColumnValueType.toMap(ExamGridColumnValueString("N")), isEmpty=false)
					} else {
						ExamGridColumnValues(ExamGridColumnValueType.toMap(ExamGridColumnValueString("Y")), isEmpty=false)
					}
				} else {
					ExamGridColumnValues(ExamGridColumnValueType.toMap(ExamGridColumnValueString("")), isEmpty=true)
				}
				_values.put(entity, values)
				_values(entity)
		}
	}

	case class MeanModuleMarkColumn(state: ExamGridColumnState)
		extends ModuleReportsColumn(state) {

		override val category: String = "Modules Report"

		override val title: String = "Mean Module Mark For This Year"

		override val secondaryValue: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		private lazy val _values = mutable.Map[ExamGridEntityYear, ExamGridColumnValues]()

		override def result(entity: ExamGridEntityYear): ExamGridColumnValues = _values.get(entity) match {
			case Some(values) => values
			case _ =>
				val entityMarks = entity.moduleRegistrations.flatMap(mr => mr.firstDefinedMark).map(mark => BigDecimal(mark))
				val values = if (entityMarks.nonEmpty) {
					ExamGridColumnValues(ExamGridColumnValueType.toMap(ExamGridColumnValueString((entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString)), isEmpty=false)
				} else {
					ExamGridColumnValues(ExamGridColumnValueType.toMap(ExamGridColumnValueString("")), isEmpty=true)
				}
				_values.put(entity, values)
				_values(entity)
		}

	}

	override def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]] = {
		state.entities.flatMap(_.years.keys).distinct.map(academicYear => academicYear -> Seq(
			PassedCoreRequiredColumn(state),
			MeanModuleMarkColumn(state)
		)).toMap
	}
}
