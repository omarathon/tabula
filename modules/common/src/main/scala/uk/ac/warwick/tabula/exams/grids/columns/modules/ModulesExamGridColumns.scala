package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.data.model.{Module, ModuleSelectionStatus, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns._

trait ModulesExamGridColumnSection extends HasExamGridColumnSection {

	self: ExamGridColumn =>

	override val sectionIdentifier: String = "modules"

	override val sectionTitleLabel: String = "Module Name"

	override val sectionSecondaryValueLabel: String = "CAT Values"

	override val sectionValueLabel: String = "Module Marks"

}

abstract class ModuleExamGridColumn(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal)
	extends ExamGridColumn(scyds) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

	override val title: String = s"${module.code.toUpperCase} ${module.name}"

	override def render: Map[String, String] =
		scyds.map(scyd => scyd.id -> {
			val modreg = scyd.moduleRegistrations.find(mr => mr.module == module && mr.cats == cats)
			modreg.map(mr => {
				if (mr.agreedMark != null) {
					if (mr.agreedGrade == "F") {
						s"<span class='exam-grid-fail'>${mr.agreedMark.toPlainString}</span>"
					} else if (mr.agreedMark.toPlainString == "0") {
						s"${mr.agreedMark.toPlainString}(${mr.agreedGrade})"
					}	else {
						mr.agreedMark.toPlainString
					}
				} else {
					"?"
				}
			}).getOrElse("")
		}).toMap

	override def renderExcelCell(
		row: XSSFRow,
		index: Int,
		scyd: StudentCourseYearDetails,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
	): Unit = {
		val cell = row.createCell(index)
		val modreg = scyd.moduleRegistrations.find(mr => mr.module == module && mr.cats == cats)
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

	final override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] = throw new UnsupportedOperationException
	def getColumns(departmentCoreRequiredModules: Seq[Module], scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn]

}

@Component
class CoreModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val sortOrder: Int = 4

	case class Column(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(scyds, module, cats) {

		override val category: String = "Core Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		scyds.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Core && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(scyds, module, cats)}

}

@Component
class CoreRequiredModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "corerequired"

	override val sortOrder: Int = 5

	case class Column(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(scyds, module, cats) {

		override val category: String = "Core Required Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		scyds.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.CoreRequired || departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(scyds, module, cats)}

}

@Component
class CoreOptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

	override val sortOrder: Int = 6

	case class Column(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(scyds, module, cats) {

		override val category: String = "Core Optional Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		scyds.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(scyds, module, cats)}

}

@Component
class OptionalModulesColumnOption extends ModulesColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "optional"

	override val sortOrder: Int = 6

	case class Column(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal) extends ModuleExamGridColumn(scyds, module, cats) {

		override val category: String = "Optional Modules"

	}

	override def getColumns(departmentCoreRequiredModules: Seq[Module], scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		scyds.flatMap(_.moduleRegistrations)
			.filter(mr => mr.selectionStatus == ModuleSelectionStatus.Option && !departmentCoreRequiredModules.contains(mr.module))
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(scyds, module, cats)}

}