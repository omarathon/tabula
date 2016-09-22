package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, ModuleSelectionStatus}
import uk.ac.warwick.tabula.exams.grids.columns._

abstract class ModuleExamGridColumn(state: ExamGridColumnState, val module: Module, cats: JBigDecimal)
	extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue {

	def moduleSelectionStatus: Option[ModuleSelectionStatus]

	override val title: String = s"${module.code.toUpperCase} ${module.name}"

	override def values: Map[ExamGridEntity, Map[YearOfStudy, ExamGridColumnValue]] = {
		state.entities.map(entity =>
			entity -> entity.years.map { case (academicYear, entityYear) =>
				academicYear -> result(entityYear)
			}
		).toMap
	}

	private def result(entity: ExamGridEntityYear): ExamGridColumnValue = {
		getModuleRegistration(entity).map(mr => {
			if (entity.markOverrides.exists(_.get(module).isDefined)) {
				ExamGridColumnValueOverride(entity.markOverrides.get(module))
			} else {
				val (mark, isActual) = mr.agreedMark match {
					case mark: JBigDecimal => (BigDecimal(mark), false)
					case _ => Option(mr.actualMark).map(m => (BigDecimal(m), true)).getOrElse(null, false)
				}
				if (mark == null) {
					ExamGridColumnValueMissing("Agreed and actual mark missing")
				} else if (isActual && mr.actualGrade == "F" || mr.agreedGrade == "F") {
					ExamGridColumnValueFailed(mark, isActual)
				} else if (entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))) {
					ExamGridColumnValueOvercat(mark, isActual)
				} else {
					ExamGridColumnValueDecimal(mark, isActual)
				}
			}
		}).getOrElse(ExamGridColumnValueString(""))
	}

	private def getModuleRegistration(entity: ExamGridEntityYear): Option[ModuleRegistration] = {
		entity.moduleRegistrations.find(mr =>
			mr.module == module &&
				mr.cats == cats &&
				(moduleSelectionStatus.isEmpty || mr.selectionStatus == moduleSelectionStatus.get)
		)
	}

	override val secondaryValue: String = cats.toPlainString

}

abstract class ModuleExamGridColumnOption extends PerYearExamGridColumnOption {

	def Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal): ModuleExamGridColumn

	def moduleRegistrationFilter(mr: ModuleRegistration, state: ExamGridColumnState): Boolean

	override final def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]] = {
		state.entities.flatMap(_.years.keys).distinct.map(academicYear => academicYear -> {
			state.entities.flatMap(_.years.get(academicYear))
				.flatMap(_.moduleRegistrations)
				.filter(moduleRegistrationFilter(_, state))
				.groupBy(mr => (mr.module, mr.cats))
				.keySet
				.toSeq.sortBy { case (module, cats) => (module, cats) }
				.map{ case (module, cats) => Column(state, module, cats)}
		}).toMap
	}
}

@Component
class CoreModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreModules

	override val mandatory = true

	class Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal) extends ModuleExamGridColumn(state, module, cats) {

		override val category: String = "Core Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.Core)

	}

	override def Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal): ModuleExamGridColumn = new Column(state, module, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, state: ExamGridColumnState): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.Core && !state.coreRequiredModules.contains(mr.module)

}

@Component
class CoreRequiredModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "corerequired"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreRequiredModules

	class Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal) extends ModuleExamGridColumn(state, module, cats) {

		override val category: String = "Core Required Modules"

		override val moduleSelectionStatus = None

	}

	override def Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal): ModuleExamGridColumn = new Column(state, module, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, state: ExamGridColumnState): Boolean =
		state.coreRequiredModules.contains(mr.module)

}

@Component
class CoreOptionalModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreOptionalModules

	class Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal) extends ModuleExamGridColumn(state, module, cats) {

		override val category: String = "Core Optional Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.OptionalCore)

	}

	override def Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal): ModuleExamGridColumn = new Column(state, module, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, state: ExamGridColumnState): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !state.coreRequiredModules.contains(mr.module)

}

@Component
class OptionalModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "optional"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.OptionalModules

	class Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal) extends ModuleExamGridColumn(state, module, cats) {

		override val category: String = "Optional Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.Option)

	}

	override def Column(state: ExamGridColumnState, module: Module, cats: JBigDecimal): ModuleExamGridColumn = new Column(state, module, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, state: ExamGridColumnState): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.Option && !state.coreRequiredModules.contains(mr.module)

}