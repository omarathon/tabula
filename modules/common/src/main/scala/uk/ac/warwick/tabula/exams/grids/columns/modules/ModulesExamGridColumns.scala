package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.apache.poi.ss.usermodel.Cell
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

@Component
class CoreModulesColumnOption extends columns.ExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val sortOrder: Int = 3

	case class Column(scyds: Seq[StudentCourseYearDetails], module: Module, cats: java.math.BigDecimal)
		extends ExamGridColumn(scyds) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with ModulesExamGridColumnSection {

		override val title: String = s"${module.code.toUpperCase} ${module.name}"

		override val category: String = "Core Modules"

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

	override def getColumns(scyds: Seq[StudentCourseYearDetails]): Seq[ExamGridColumn] =
		scyds.flatMap(_.moduleRegistrations)
			.filter(_.selectionStatus == ModuleSelectionStatus.Core)
			.groupBy(mr => (mr.module, mr.cats))
			.keySet
			.toSeq.sortBy(mrc => (mrc._1, mrc._2))
			.map{case(module, cats) => Column(scyds, module, cats)}

}