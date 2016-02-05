package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, Module, ModuleSelectionStatus}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns._

trait ModulesExamGridColumnSection extends HasExamGridColumnSection {

	self: ExamGridColumn =>

	override val sectionIdentifier: String = "modules"

	override val sectionTitleLabel: String = "Module Name"

	override val sectionSecondaryValueLabel: String = "CAT Values"

	override val sectionValueLabel: String = "Module Marks"

}

abstract class ModuleExamGridColumn(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal)
	extends ExamGridColumn(entities) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

	override val title: String = s"${module.code.toUpperCase} ${module.name}"

	override def render: Map[String, String] =
		entities.map(entity => entity.id -> {
			val modreg = entity.moduleRegistrations.find(mr => mr.module == module && mr.cats == cats)
			modreg.map(mr => {
				val mark = markWithOverride(entity, mr)
				if (mark != null) {
					// entity.studentCourseYearDetails.isDefined checks if this is a real SCYD or just an entity for showing overcatting options
					// If the latter we don't want to highlight if it's used (because in that case they all are)
					val usedInOvercattingClass = if (entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))) "exam-grid-overcat" else ""
					val failedClass = if (mr.agreedGrade == "F") "exam-grid-fail" else ""
					val overriddenClass = if (entity.markOverrides.flatMap(_.get(module)).isDefined) "exam-grid-override" else ""
					val append = if (mark.toString == "0") s"(${mr.agreedGrade})" else ""

					"<span class=\"%s\">%s%s</span>".format(Seq(usedInOvercattingClass, failedClass, overriddenClass).mkString(" "), mark.toString, append)
				} else {
					"?"
				}
			}).getOrElse("")
		}).toMap

	override def renderExcelCell(
		row: XSSFRow,
		index: Int,
		entity: GenerateExamGridEntity,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
	): Unit = {
		val cell = row.createCell(index)
		val modreg = entity.moduleRegistrations.find(mr => mr.module == module && mr.cats == cats)
		modreg.foreach(mr => {
			val mark = markWithOverride(entity, mr)
			if (mark != null) {
				val usedInOvercatting = entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))
				val isFailed = mr.agreedGrade == "F"
				val isOverridden = entity.markOverrides.flatMap(_.get(module)).isDefined
				val append = if (mark.toString == "0") s"(${mr.agreedGrade})" else ""

				if (usedInOvercatting || isOverridden || append.length > 0) {
					if (isOverridden) {
						cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overridden))
					} else if (usedInOvercatting) {
						cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overcat))
					}
					cell.setCellValue("%s%s%s%s%s".format(
						if (isOverridden) "{" else "",
						mark.toString,
						append,
						if (isOverridden) "}" else "",
						if (usedInOvercatting) "*" else ""
					))
				} else {
					if (isFailed) {
						cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Fail))
					}
					cell.setCellValue(mark.doubleValue)
				}
			} else {
				cell.setCellValue("?")
			}
		})
	}

	private def markWithOverride(entity: GenerateExamGridEntity, moduleRegistration: ModuleRegistration): BigDecimal = {
		entity.markOverrides.getOrElse(Map()).getOrElse(module, Option(moduleRegistration.agreedMark).map(m => BigDecimal(m)).orNull)
	}

	override val renderSecondaryValue: String = cats.toPlainString

}

trait ModulesColumnOption extends columns.ExamGridColumnOption {

	final override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = throw new UnsupportedOperationException
	def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn]

}

@Component
class CoreModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreModules

	override val mandatory = true

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Modules"

	}

	override def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Core && !coreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class CoreRequiredModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "corerequired"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreRequiredModules

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Required Modules"

	}

	override def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => coreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class CoreOptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreOptionalModules

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Optional Modules"

	}

	override def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !coreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class OptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "optional"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.OptionalModules

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Optional Modules"

	}

	override def getColumns(coreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Option && !coreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}