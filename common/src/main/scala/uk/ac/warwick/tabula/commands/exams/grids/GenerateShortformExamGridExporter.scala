package uk.ac.warwick.tabula.commands.exams.grids


import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{ModuleExamGridColumn, ModuleReportsColumn}
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

object GenerateShortformExamGridExporter {

	import ExamGridExportStyles._

	def apply(
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoadLookup: NormalLoadLookup,
		entities: Seq[ExamGridEntity],
		leftColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[StudentCourseYearDetails.YearOfStudy, Seq[PerYearExamGridColumn]],
		rightColumns: Seq[ChosenYearExamGridColumn],
		chosenYearColumnValues: Map[ChosenYearExamGridColumn, Map[ExamGridEntity, ExamGridColumnValue]],
		perYearColumnValues: Map[PerYearExamGridColumn, Map[ExamGridEntity, Map[StudentCourseYearDetails.YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]],
		moduleColumnsPerEntity: Map[ExamGridEntity, Map[YearOfStudy, Seq[Option[ModuleExamGridColumn]]]],
		perYearModuleMarkColumns: Map[YearOfStudy, Seq[ModuleExamGridColumn]],
		perYearModuleReportColumns: Map[YearOfStudy, Seq[ModuleReportsColumn]],
		maxYearColumnSize: Map[YearOfStudy, Int],
		showComponentMarks: Boolean,
		yearOrder: Ordering[Int] = Ordering.Int
	): Workbook = {
		// Allow randomly accessing rows at any point during generation, don't flush
		val workbook = new SXSSFWorkbook(null, -1)

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))
		sheet.trackAllColumnsForAutoSizing()

		summaryAndKey(sheet, cellStyleMap, department, academicYear, course, routes, yearOfStudy, yearWeightings, normalLoadLookup, entities.size, isStudentCount = true)

		// CREATE ROWS
		val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		val entityRows = entities.map(entity => entity -> {
			val entityHeader = sheet.createRow(sheet.getLastRowNum + 1)
			val valueRows = if (showComponentMarks) {
				Map[ExamGridColumnValueType, Row](
					ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Assignment -> sheet.createRow(sheet.getLastRowNum + 1),
					ExamGridColumnValueType.Exam -> sheet.createRow(sheet.getLastRowNum + 1)
				)
			} else {
				Map[ExamGridColumnValueType, Row](ExamGridColumnValueType.Overall -> sheet.createRow(sheet.getLastRowNum + 1))
			}
			(entityHeader, valueRows)
		}).toMap

		val chosenYearColumnCategories = rightColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

		var currentColumnIndex = 3 // Move to the right of the key
		var categoryRowMaxCellWidth = 0
		var headerRowMaxCellWidth = 0
		var entityHeaderRowMaxCellWidth = 0

		// LEFT COLUMNS
		leftColumns.foreach(leftColumn => {
			// Nothing in category row
			// Header row
			val headerCell = headerRow.createCell(currentColumnIndex)
			headerCell.setCellValue(leftColumn.title)
			sheet.autoSizeColumn(currentColumnIndex)
			headerCell.setCellStyle(cellStyleMap(Header))

			// Nothing in secondary value row
			// Entity rows
			entities.foreach(entity =>
				if (chosenYearColumnValues.get(leftColumn).exists(_.get(entity).isDefined)) {
					val (header, _) = entityRows(entity)
					val entityCell = header.createCell(currentColumnIndex)
					chosenYearColumnValues(leftColumn)(entity).populateCell(entityCell, cellStyleMap)
					if (showComponentMarks) {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 3, entityCell.getColumnIndex, entityCell.getColumnIndex))
					} else {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 1, entityCell.getColumnIndex, entityCell.getColumnIndex))
					}
				}
			)
			// And finally...
			sheet.setColumnWidth(currentColumnIndex, leftColumn.excelColumnWidth)
			currentColumnIndex = currentColumnIndex + 1
		})

		if (!showComponentMarks) {
			// Add a spacer
			sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
			currentColumnIndex = currentColumnIndex + 1
		}

		// PER YEAR COLUMNS
		perYearColumns.keys.toSeq.sorted(yearOrder).foreach(year => {
			if (showComponentMarks) {
				entityRows.foreach { case (_, (header, rowMap)) => rowMap.foreach { case (valueType, row) =>
					val cell = row.createCell(currentColumnIndex)
					cell.setCellValue(valueType.label)
				}}
				sheet.setColumnWidth(currentColumnIndex, ExamGridColumnOption.ExcelColumnSizes.Spacer)
				currentColumnIndex = currentColumnIndex + 1
			}

			// for each module column
			for(moduleColumnIndex <- 1 to maxYearColumnSize(year)) {
				// Year heading
				if(moduleColumnIndex == 1){
					val headerCell = headerRow.createCell(currentColumnIndex)
					headerCell.setCellValue(s"Year $year")
					headerCell.setCellStyle(cellStyleMap(Header))
					headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
					val yearWidth = maxYearColumnSize(year) - 1
					sheet.addMergedRegion(new CellRangeAddress(headerCell.getRowIndex, headerCell.getRowIndex, headerCell.getColumnIndex, headerCell.getColumnIndex + yearWidth))
				}

				// Entity rows
				entities.foreach(entity => {
					moduleColumnsPerEntity(entity)(year).lift(moduleColumnIndex-1).foreach(col => {
						val (header, valueRows) = entityRows(entity)
						col match {
							// has marks for this module
							case Some(column) if perYearColumnValues.get(column).exists(_.get(entity).exists(_.get(year).isDefined)) =>
								val column = col.get
								val headerCell = header.createCell(currentColumnIndex)
								val title = s"${column.title} - ${column.secondaryValue} ${column.categoryShortForm}"
								headerCell.setCellValue(title)
								sheet.autoSizeColumn(currentColumnIndex)
								entityHeaderRowMaxCellWidth = Math.max(entityHeaderRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
								headerCell.setCellStyle(cellStyleMap(Rotated))

								if (showComponentMarks) {
									val overallCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
									perYearColumnValues(column)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(overallCell, cellStyleMap)
									val assignmentCell = valueRows(ExamGridColumnValueType.Assignment).createCell(currentColumnIndex)
									ExamGridColumnValue.merge(perYearColumnValues(column)(entity)(year)(ExamGridColumnValueType.Assignment)).populateCell(assignmentCell, cellStyleMap)
									val examsCell = valueRows(ExamGridColumnValueType.Exam).createCell(currentColumnIndex)
									ExamGridColumnValue.merge(perYearColumnValues(column)(entity)(year)(ExamGridColumnValueType.Exam)).populateCell(examsCell, cellStyleMap)
								} else {
									val entityCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
									perYearColumnValues(column)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(entityCell, cellStyleMap)
								}

							// blank cell(s)
							case _ =>
								if (showComponentMarks) {
									valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
								} else {
									valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
									valueRows(ExamGridColumnValueType.Assignment).createCell(currentColumnIndex)
									valueRows(ExamGridColumnValueType.Exam).createCell(currentColumnIndex)
								}
						}
					})
				})

				// and finally ..
				sheet.setColumnWidth(currentColumnIndex, perYearModuleMarkColumns(year).head.excelColumnWidth)
				currentColumnIndex = currentColumnIndex + 1
			}

			// Module report columns
			perYearModuleReportColumns(year).foreach(reportColumn => {
				// header row
				val headerCell = headerRow.createCell(currentColumnIndex)
				headerCell.setCellValue(reportColumn.title)
				headerCell.setCellStyle(cellStyleMap(Rotated))
				sheet.autoSizeColumn(currentColumnIndex)
				headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
				// Entity rows
				entities.foreach(entity => {
					// empty header
					val (header, valueRows) = entityRows(entity)
					header.createCell(currentColumnIndex)
					val overallCell = valueRows(ExamGridColumnValueType.Overall).createCell(currentColumnIndex)
					if (perYearColumnValues.get(reportColumn).exists(_.get(entity).exists(_.get(year).isDefined))) {
						perYearColumnValues(reportColumn)(entity)(year)(ExamGridColumnValueType.Overall).head.populateCell(overallCell, cellStyleMap)
					}
				})
				// and finally ..
				sheet.setColumnWidth(currentColumnIndex, reportColumn.excelColumnWidth)
				currentColumnIndex = currentColumnIndex + 1
			})
		})

		// RIGHT COLUMNS
		var currentCategory = ""
		rightColumns.foreach(rightColumn => {
			// Category row
			rightColumn match {
				case hasCategory: HasExamGridColumnCategory if hasCategory.category != currentCategory =>
					currentCategory = hasCategory.category

					val categoryCell = categoryRow.createCell(currentColumnIndex)
					categoryCell.setCellValue(hasCategory.category)
					sheet.autoSizeColumn(currentColumnIndex)
					categoryRowMaxCellWidth = Math.max(categoryRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))
					categoryCell.setCellStyle(cellStyleMap(HeaderRotated))

					// Guard against trying to create a merged region with only one cell in it
					val startColumn = categoryCell.getColumnIndex
					val endColumn = categoryCell.getColumnIndex + chosenYearColumnCategories(hasCategory.category).size - 1

					if (endColumn > startColumn)
						sheet.addMergedRegion(new CellRangeAddress(categoryCell.getRowIndex, categoryCell.getRowIndex, startColumn, endColumn))
				case _ =>
			}

			// Header row
			val headerCell = headerRow.createCell(currentColumnIndex)
			headerCell.setCellValue(rightColumn.title)
			sheet.autoSizeColumn(currentColumnIndex)
			headerRowMaxCellWidth = Math.max(headerRowMaxCellWidth, sheet.getColumnWidth(currentColumnIndex))

			if(rightColumn.boldTitle)
				headerCell.setCellStyle(cellStyleMap(HeaderRotated))
			else
				headerCell.setCellStyle(cellStyleMap(Rotated))


			// Entity rows
			entities.foreach(entity => {
				val (header, _) = entityRows(entity)
				if (chosenYearColumnValues.get(rightColumn).exists(_.get(entity).isDefined)) {
					val entityCell = header.createCell(currentColumnIndex)
					chosenYearColumnValues(rightColumn)(entity).populateCell(entityCell, cellStyleMap)
					if (showComponentMarks) {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 3, entityCell.getColumnIndex, entityCell.getColumnIndex))
					} else {
						sheet.addMergedRegion(new CellRangeAddress(entityCell.getRowIndex, entityCell.getRowIndex + 1, entityCell.getColumnIndex, entityCell.getColumnIndex))
					}
				}
			})

			// And finally...
			sheet.setColumnWidth(currentColumnIndex, rightColumn.excelColumnWidth)
			currentColumnIndex = currentColumnIndex + 1
		})

		categoryRow.setHeight(Math.min(4000, categoryRowMaxCellWidth * 0.5).toShort)
		headerRow.setHeight(Math.min(4000, headerRowMaxCellWidth * 0.5).toShort)
		entityRows.values.map{ case (header, _) => header }.foreach(entityHeader =>
			entityHeader.setHeight(Math.min(4000, entityHeaderRowMaxCellWidth * 0.5).toShort)
		)

		workbook
	}

	private def summaryAndKey(
		sheet: Sheet,
		cellStyleMap: Map[Style, CellStyle],
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		routes: Seq[Route],
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoadLookup: NormalLoadLookup,
		count: Int,
		isStudentCount: Boolean
	): Unit = {
		def keyValueCells(key: String, value: String, rowIndex: Int) = {
			val row = sheet.createRow(rowIndex)
			val keyCell = row.createCell(0)
			keyCell.setCellValue(key)
			keyCell.setCellStyle(cellStyleMap(Header))
			val valueCell = row.createCell(1)
			valueCell.setCellValue(value)
			row
		}
		keyValueCells("Department:", department.name, 0)
		keyValueCells("Academic year:", academicYear.toString, 1)
		keyValueCells("Course:", s"${course.code.toUpperCase} ${course.name}", 2)
		routes.size match {
			case 0 => keyValueCells("Routes:", "All routes", 3)
			case 1 => keyValueCells("Route:", s"${routes.head.code.toUpperCase} ${routes.head.name}", 3)
			case n => keyValueCells("Routes:", s"$n routes", 3)
		}
		keyValueCells("Year of study:", yearOfStudy.toString, 4)
		val yearWeightingRow = keyValueCells("Year weightings:", yearWeightings.map(cyw => s"Year ${cyw.yearOfStudy} = ${cyw.weightingAsPercentage}%").mkString("\n"), 5)
		yearWeightingRow.setHeight((yearWeightingRow.getHeight * (yearWeightings.size - 1)).toShort)
		val normalCATSLoadRow = keyValueCells("Normal CATS load:", normalLoadLookup.routes.sortBy(_.code).map(r => s"${r.code.toUpperCase}: ${normalLoadLookup(r).underlying.toString}").mkString("\n"), 6)
		normalCATSLoadRow.setHeight((normalCATSLoadRow.getHeight * (normalLoadLookup.routes.size - 1)).toShort)
		if (isStudentCount) {
			keyValueCells("Student Count:", count.toString, 7)
		} else {
			keyValueCells("Count:", count.toString, 7)
		}
		keyValueCells("Grid Generated:", DateTime.now.toString, 8)

		{
			val row = sheet.createRow(9)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(Fail))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Failed module")
		}
		{
			val row = sheet.createRow(10)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(Overcat))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Used in overcatting calculation")
		}
		{
			val row = sheet.createRow(11)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ActualMark))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark missing, using actual")
		}
		{
			val row = sheet.createRow(12)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("X")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark and actual mark missing")
		}
		{
			val row = sheet.createRow(13)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Blank indicates module not taken by student")
		}
		{
			val row = sheet.createRow(14)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("AB")
			keyCell.setCellStyle(cellStyleMap(BoldText))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Bold module name indicates a duplicate table entry")
		}

		sheet.autoSizeColumn(0)
		sheet.autoSizeColumn(1)
	}

}
