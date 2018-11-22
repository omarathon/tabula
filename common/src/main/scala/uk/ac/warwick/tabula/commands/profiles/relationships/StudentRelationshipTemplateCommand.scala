package uk.ac.warwick.tabula.commands.profiles.relationships

import org.apache.poi.ss.usermodel.{Row, Sheet}
import org.apache.poi.ss.util.{CellRangeAddressList, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, AutowiringUserLookupComponent, RelationshipServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

object StudentRelationshipTemplateCommand {

	val agentLookupSheetName = "AgentLookup"
	val sheetPassword = "roygbiv"

	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new StudentRelationshipTemplateCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with AutowiringUserLookupComponent
			with ComposableCommand[ExcelView]
			with StudentRelationshipTemplatePermissions
			with StudentRelationshipTemplateCommandState
			with StudentRelationshipTemplateCommandRequest
			with ReadOnly with Unaudited
}


class StudentRelationshipTemplateCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[ExcelView] {

	self: RelationshipServiceComponent with UserLookupComponent with StudentRelationshipTemplateCommandRequest =>

	override def applyInternal(): ExcelView = {
		val dbUnallocated = relationshipService.getStudentAssociationDataWithoutRelationship(department, relationshipType)
		val dbAllocated = relationshipService.getStudentAssociationEntityData(department, relationshipType, additionalEntities.asScala)

		val (unallocated, allocationsForTemplate) = {
			if (templateWithChanges) {
				val allocatedWithRemovals = dbAllocated.map(entityData => {
					if (removals.containsKey(entityData.entityId)) {
						val theseRemovals = removals.get(entityData.entityId).asScala
						entityData.updateStudents(entityData.students.filterNot(student => theseRemovals.contains(student.universityId)))
					} else {
						entityData
					}
				})

				val allocatedWithAdditionsAndRemovals = allocatedWithRemovals.map(entityData => {
					if (additions.containsKey(entityData.entityId)) {
						entityData.updateStudents(entityData.students ++ additions.get(entityData.entityId).asScala.flatMap(universityId => dbUnallocated.find(_.universityId == universityId)))
					} else {
						entityData
					}
				})

				val allDbAllocatedStudents = dbAllocated.flatMap(_.students).distinct
				val allAllocatedStudents = allocatedWithAdditionsAndRemovals.flatMap(_.students).distinct
				val newUnallocated = allDbAllocatedStudents.filterNot(allAllocatedStudents.contains)
				val newlyAllocatedUniIds = additions.asScala.mapValues(_.asScala).values.flatten.toSeq
				val unallocatedWithAdditionsAndRemovals =  newUnallocated ++ dbUnallocated.filterNot(student => newlyAllocatedUniIds.contains(student.universityId))
				(unallocatedWithAdditionsAndRemovals, allocatedWithAdditionsAndRemovals)
			} else {
				(dbUnallocated, dbAllocated)
			}
		}

		new ExcelView("Allocation for " + allocateSheetName + ".xlsx", generateWorkbook(unallocated, allocationsForTemplate))
	}

	private def generateWorkbook(unallocated: Seq[StudentAssociationData], allocations: Seq[StudentAssociationEntityData]) = {
		val workbook = new SXSSFWorkbook
		val sheet = generateAllocationSheet(workbook)
		val allUniversityIds = allocations.map(_.entityId) ++ allocations.flatMap(_.students.map(_.universityId)) ++ unallocated.map(_.universityId)
		val usercodes = userLookup.getUsersByWarwickUniIds(allUniversityIds).mapValues(_.getUserId)

		generateAgentLookupSheet(workbook, allocations, usercodes)
		generateAgentDropdowns(sheet, allocations, unallocated)

		val agentLookupRange = StudentRelationshipTemplateCommand.agentLookupSheetName + "!$A2:$C" + (allocations.length + 1)

		allocations.foreach{ agent =>
			agent.students.foreach{ student =>
				val row = sheet.createRow(sheet.getLastRowNum + 1)

				row.createCell(0).setCellValue(student.universityId)
				row.createCell(1).setCellValue(s"${student.firstName} ${student.lastName}")
				row.createCell(2).setCellValue(usercodes.getOrElse(student.universityId, ""))

				val agentNameCell = createUnprotectedCell(workbook, row, 3) // unprotect cell for the dropdown agent name
				agentNameCell.setCellValue(agent.displayName)

				row.createCell(4).setCellFormula(
					"IF(AND(ISTEXT($D" + (row.getRowNum + 1) + "), LEN($D" + (row.getRowNum + 1) + ") > 0), VLOOKUP($D" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
				)

				row.createCell(5).setCellFormula(
					"IF(AND(ISTEXT($D" + (row.getRowNum + 1) + "), LEN($D" + (row.getRowNum + 1) + ") > 0), VLOOKUP($D" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 3, FALSE), \" \")"
				)
			}
		}

		unallocated.foreach{ student =>
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			row.createCell(0).setCellValue(student.universityId)
			row.createCell(1).setCellValue(s"${student.firstName} ${student.lastName}")
			row.createCell(2).setCellValue(usercodes.getOrElse(student.universityId, ""))

			createUnprotectedCell(workbook, row, 3) // unprotect cell for the dropdown agent name

			row.createCell(4).setCellFormula(
				"IF(AND(ISTEXT($D" + (row.getRowNum + 1) + "), LEN($D" + (row.getRowNum + 1) + ") > 0), VLOOKUP($D" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
			)

			row.createCell(5).setCellFormula(
				"IF(AND(ISTEXT($D" + (row.getRowNum + 1) + "), LEN($D" + (row.getRowNum + 1) + ") > 0), VLOOKUP($D" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 3, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}

	private def generateAgentLookupSheet(workbook: SXSSFWorkbook, allocations: Seq[StudentAssociationEntityData], usercodes: Map[UniversityId, String] ) = {
		val agentSheet = workbook.createSheet(StudentRelationshipTemplateCommand.agentLookupSheetName)

		for (agent <- allocations) {
			val row = agentSheet.createRow(agentSheet.getLastRowNum + 1)
			row.createCell(0).setCellValue(agent.displayName)
			row.createCell(1).setCellValue(usercodes.getOrElse(agent.entityId, ""))
			row.createCell(2).setCellValue(agent.entityId)
		}

		agentSheet.protectSheet(StudentRelationshipTemplateCommand.sheetPassword)
		agentSheet
	}

	// attaches the data validation to the sheet
	private def generateAgentDropdowns(sheet: Sheet, allocations: Seq[StudentAssociationEntityData], unallocated: Seq[StudentAssociationData]) {
		if (allocations.nonEmpty) {
			val dropdownRange = new CellRangeAddressList(1, allocations.flatMap(_.students).length + unallocated.length, 3, 3)
			val validation = getDataValidation(allocations, sheet, dropdownRange)

			sheet.addValidationData(validation)
		}
	}

	// Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
	private def getDataValidation(allocations: Seq[StudentAssociationEntityData], sheet: Sheet, addressList: CellRangeAddressList) = {
		val dvHelper = new XSSFDataValidationHelper(null)
		val dvConstraint = dvHelper.createFormulaListConstraint(StudentRelationshipTemplateCommand.agentLookupSheetName + "!$A$2:$A$" + (allocations.length + 1))
		val validation = dvHelper.createValidation(dvConstraint, addressList)

		validation.setShowErrorBox(true)
		validation
	}

	private def createUnprotectedCell(workbook: SXSSFWorkbook, row: Row, col: Int, value: String = "") = {
		val lockedCellStyle = workbook.createCellStyle()
		lockedCellStyle.setLocked(false)
		val cell = row.createCell(col)
		cell.setCellValue(value)
		cell.setCellStyle(lockedCellStyle)
		cell
	}

	private def formatWorkbook(workbook: SXSSFWorkbook): Unit = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))

		val sheet = workbook.getSheet(allocateSheetName)

		// set style on all columns
		0 to 3 foreach  { col =>
			sheet.setDefaultColumnStyle(col, style)
			sheet.autoSizeColumn(col)
		}

		// set ID column to be wider
		sheet.setColumnWidth(3, 7000)

	}

	private def generateAllocationSheet(workbook: SXSSFWorkbook): Sheet =  {
		val sheet = workbook.createSheet(allocateSheetName)
		sheet.trackAllColumnsForAutoSizing()

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue(s"${relationshipType.studentRole.capitalize} name")
		header.createCell(2).setCellValue(s"${relationshipType.studentRole.capitalize} usercode")
		header.createCell(3).setCellValue(s"${relationshipType.agentRole.capitalize} name")
		header.createCell(4).setCellValue(s"${relationshipType.agentRole.capitalize} usercode")
		header.createCell(5).setCellValue("agent_id")


		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(StudentRelationshipTemplateCommand.sheetPassword)
		sheet
	}

	def allocateSheetName: String = trimmedSheetName(relationshipType.agentRole.capitalize + "s for " + department.name)

	// Excel sheet names must be 31 chars or less so
	private def trimmedSheetName(rawSheetName: String) = {
		val sheetName = WorkbookUtil.createSafeSheetName(rawSheetName)

		if (sheetName.length > 31) sheetName.substring(0, 31)
		else sheetName
	}

}

trait StudentRelationshipTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentRelationshipTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait StudentRelationshipTemplateCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}

trait StudentRelationshipTemplateCommandRequest {
	var additions: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	var removals: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	var additionalEntities: JList[String] = JArrayList()

	var templateWithChanges = false
}
