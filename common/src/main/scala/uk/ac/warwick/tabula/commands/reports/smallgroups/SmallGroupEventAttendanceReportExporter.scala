package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.tabula.commands.reports.ReportsDateFormats
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class SmallGroupEventAttendanceReportExporter(val processorResult: Seq[SmallGroupEventAttendanceReportProcessorResult], val department: Department)
  extends CSVLineWriter[SmallGroupEventAttendanceReportProcessorResult] {

  val headers: Seq[String] = Seq(
    "Module code",
    "Small group set",
    "Small group",
    "Week",
    "Day",
    "Event date",
    "Location",
    "Staff",
    "Recorded students",
    "Unrecorded students",
    "Earliest recorded attendance",
    "Latest recorded attendance"
  )

  override def getNoOfColumns(o: SmallGroupEventAttendanceReportProcessorResult): Int = headers.size

  override def getColumn(result: SmallGroupEventAttendanceReportProcessorResult, index: Int): String = {
    index match {
      case 0 =>
        result.moduleCode
      case 1 =>
        result.setName
      case 2 =>
        result.groupName
      case 3 =>
        result.week.toString
      case 4 =>
        result.dayString
      case 5 =>
        ReportsDateFormats.ReportDate.print(result.eventDate)
      case 6 =>
        result.location
      case 7 =>
        result.tutors
      case 8 =>
        result.recorded.toString
      case 9 =>
        result.unrecorded.toString
      case 10 =>
        result.earliestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull
      case 11 =>
        result.latestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull
    }
  }

  def toXLSX: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = generateNewSheet(workbook)

    processorResult.foreach(addRow(sheet))

    (0 to headers.size) foreach sheet.autoSizeColumn
    workbook
  }

  private def generateNewSheet(workbook: SXSSFWorkbook) = {
    val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(department.name))
    sheet.trackAllColumnsForAutoSizing()

    // add header row
    val headerRow = sheet.createRow(0)
    headers.zipWithIndex foreach {
      case (header, index) => headerRow.createCell(index).setCellValue(header)
    }
    sheet
  }

  private def addRow(sheet: Sheet)(result: SmallGroupEventAttendanceReportProcessorResult): Unit = {
    val plainCellStyle = {
      val cs = sheet.getWorkbook.createCellStyle()
      cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
      cs
    }

    val row = sheet.createRow(sheet.getLastRowNum + 1)
    headers.zipWithIndex foreach { case (_, index) =>
      val cell = row.createCell(index)

      if (index == 2) {
        // University IDs have leading zeros and Excel would normally remove them.
        // Set a manual data format to remove this possibility
        cell.setCellStyle(plainCellStyle)
      }

      cell.setCellValue(getColumn(result, index))
    }
  }

  def toXML: Elem = {
    <result>
      <events>
        {processorResult.map(event =>
          <event
          id={event.id}
          moduleCode={event.moduleCode}
          setName={event.setName}
          format={event.format}
          groupName={event.groupName}
          week={event.week.toString}
          day={event.day.toString}
          date={ReportsDateFormats.ReportDate.print(event.eventDate)}
          location={event.location}
          tutors={event.tutors}
          recorded={event.recorded.toString}
          unrecorded={event.unrecorded.toString}
          earliestRecordedAttendance={event.earliestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull}
          latestRecordedAttendance={event.latestRecordedAttendance.map(ReportsDateFormats.ReportDate.print).orNull}/>
      )}
      </events>
    </result>
  }
}
