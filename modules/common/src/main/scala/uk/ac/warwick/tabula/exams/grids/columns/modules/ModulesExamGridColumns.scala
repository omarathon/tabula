package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{GenerateExamGridEntity, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.{Module, ModuleSelectionStatus}
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
				if (mr.agreedMark != null) {
					// entity.studentCourseYearDetails.isDefined checks if this is a real SCYD or just an entity for showing overcatting options
					// If the latter we don't want to highlight if it's used (because in that case they all are)
					val isUsedInOvercatting = entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))
					val isFailed = mr.agreedGrade == "F"
					val classes = {
						if (isUsedInOvercatting && isFailed) {
							"exam-grid-fail exam-grid-overcat"
						} else if (isUsedInOvercatting) {
							"exam-grid-overcat"
						} else if (isFailed) {
							"exam-grid-fail"
						} else {
							""
						}
					}
					val append = {
						if (mr.agreedMark.toPlainString == "0") {
							s"(${mr.agreedGrade})"
						} else {
							""
						}
					}
					"<span class=\"%s\">%s%s</span>".format(classes, mr.agreedMark.toPlainString, append)
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
			if (mr.agreedMark != null) {
				if (mr.agreedGrade == "F") {
					cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Fail))
					cell.setCellValue(mr.agreedMark.doubleValue)
				} else if (mr.agreedMark.toPlainString == "0") {
					cell.setCellValue(s"${mr.agreedMark.toPlainString}(${mr.agreedGrade})")
				}	else {
					cell.setCellValue(mr.agreedMark.doubleValue)
				}
			} else {
				cell.setCellValue("?")
			}
		})
	}

	override val renderSecondaryValue: String = cats.toPlainString

}

trait ModulesColumnOption extends columns.ExamGridColumnOption {

	final override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = throw new UnsupportedOperationException
	def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn]

}

@Component
class CoreModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val sortOrder: Int = 4

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Core && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class CoreRequiredModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "corerequired"

	override val sortOrder: Int = 5

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Required Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.CoreRequired || departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class CoreOptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

	override val sortOrder: Int = 6

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Core Optional Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}

@Component
class OptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "optional"

	override val sortOrder: Int = 6

	case class Column(entities: Seq[GenerateExamGridEntity], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(entities, module, cats) {

		override val category: String = "Optional Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] =
		entities.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Option && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(entities, module, cats)}

}