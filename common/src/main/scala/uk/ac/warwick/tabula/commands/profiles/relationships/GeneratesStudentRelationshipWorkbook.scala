package uk.ac.warwick.tabula.commands.profiles.relationships

import org.apache.poi.ss.usermodel.{Row, Sheet}
import org.apache.poi.ss.util.{CellRangeAddressList, WorkbookUtil}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}

trait GeneratesStudentRelationshipWorkbook {

  private val agentLookupSheetName = "AgentLookup"
  private val sheetPassword = "roygbiv"

  def generateWorkbook(allAgents: Seq[Member], allAllocations: Seq[(Member, Seq[Member])], department: Department, relationshipType: StudentRelationshipType): SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateAllocationSheet(workbook, department, relationshipType)
    generateAgentLookupSheet(workbook, allAgents)
    generateAgentDropdowns(sheet, allAgents, allAllocations)

    val agentLookupRange = agentLookupSheetName + "!$A2:$B" + (allAgents.length + 1)

    allAllocations
      // TAB-2677
      .filter { case (_, agents) => agents.size <= 1 }
      .foreach { case (student, agents) =>
        val row = sheet.createRow(sheet.getLastRowNum + 1)

        row.createCell(0).setCellValue(student.universityId)
        row.createCell(1).setCellValue(student.fullName.getOrElse(""))

        val agentNameCell = createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown agent name
        agents.headOption.flatMap(_.fullName).foreach(agentNameCell.setCellValue)

        row.createCell(3).setCellFormula(
          "IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
        )
        workbook.getCreationHelper.createFormulaEvaluator().evaluateInCell(row.getCell(3))
      }

    formatWorkbook(workbook, department, relationshipType)
    workbook
  }

  private def generateAgentLookupSheet(workbook: SXSSFWorkbook, agents: Seq[Member]) = {
    val agentSheet = workbook.createSheet(agentLookupSheetName)

    for (agent <- agents) {
      val row = agentSheet.createRow(agentSheet.getLastRowNum + 1)
      row.createCell(0).setCellValue(agent.fullName.getOrElse(agent.universityId))
      row.createCell(1).setCellValue(agent.universityId)
    }

    agentSheet.protectSheet(sheetPassword)
    agentSheet
  }

  // attaches the data validation to the sheet
  private def generateAgentDropdowns(sheet: Sheet, agents: Seq[_], allAllocations: Seq[_]) {
    if (agents.nonEmpty) {
      val dropdownRange = new CellRangeAddressList(1, allAllocations.length, 2, 2)
      val validation = getDataValidation(agents, sheet, dropdownRange)

      sheet.addValidationData(validation)
    }
  }

  // Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
  private def getDataValidation(agents: Seq[_], sheet: Sheet, addressList: CellRangeAddressList) = {
    val dvHelper = new XSSFDataValidationHelper(null)
    val dvConstraint = dvHelper.createFormulaListConstraint(agentLookupSheetName + "!$A$2:$A$" + (agents.length + 1))
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

  private def formatWorkbook(workbook: SXSSFWorkbook, department: Department, relationshipType: StudentRelationshipType) = {
    val style = workbook.createCellStyle
    val format = workbook.createDataFormat

    // using an @ sets text format (from BuiltinFormats.class)
    style.setDataFormat(format.getFormat("@"))

    val sheet = workbook.getSheet(allocateSheetName(department, relationshipType))

    // set style on all columns
    0 to 3 foreach { col =>
      sheet.setDefaultColumnStyle(col, style)
      sheet.autoSizeColumn(col)
    }

    // set ID column to be wider
    sheet.setColumnWidth(3, 7000)

  }

  private def generateAllocationSheet(workbook: SXSSFWorkbook, department: Department, relationshipType: StudentRelationshipType): Sheet = {
    val sheet = workbook.createSheet(allocateSheetName(department, relationshipType))
    sheet.trackAllColumnsForAutoSizing()

    // add header row
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("student_id")
    header.createCell(1).setCellValue(relationshipType.studentRole.capitalize + " name")
    header.createCell(2).setCellValue(relationshipType.agentRole.capitalize + " name")
    header.createCell(3).setCellValue("agent_id")

    // using apache-poi, we can't protect certain cells - rather we have to protect
    // the entire sheet and then unprotect the ones we want to remain editable
    sheet.protectSheet(sheetPassword)
    sheet
  }

  def allocateSheetName(department: Department, relationshipType: StudentRelationshipType): String =
    trimmedSheetName(relationshipType.agentRole.capitalize + "s for " + department.name)

  // Excel sheet names must be 31 chars or less so
  private def trimmedSheetName(rawSheetName: String) = {
    val sheetName = WorkbookUtil.createSafeSheetName(rawSheetName)

    if (sheetName.length > 31) sheetName.substring(0, 31)
    else sheetName
  }

}
