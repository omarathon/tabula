package uk.ac.warwick.tabula.commands.reports.cm2

import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

object MissedAssessmentsReportCsvExporter extends CSVLineWriter[MissedAssessmentsReportEntity] {
  val headers: Seq[String] = Seq(
    "First name",
    "Last name",
    "University ID",
    "Module",
    "Assignment",
    "Extension",
    "Working days late"
  )

  override def getNoOfColumns(o: MissedAssessmentsReportEntity): Int = 7

  override def getColumn(o: MissedAssessmentsReportEntity, col: Int): String = col match {
    case 0 => o.student.firstName
    case 1 => o.student.lastName
    case 2 => o.student.universityId
    case 3 => o.module.code.toUpperCase()
    case 4 => o.assignment.name
    case 5 => o.extension.flatMap(_.expiryDate).map(_.toString).getOrElse("None")
    case 6 => o.workingDaysLate.toString
  }
}

class MissedAssessmentsReportXlsxExporter(report: MissedAssessmentsReport) {
  def toXLSX: SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    val sheet = workbook.createSheet()
    sheet.trackAllColumnsForAutoSizing()

    // add header row
    val headerRow = sheet.createRow(0)
    headers.zipWithIndex.foreach {
      case (header, index) => headerRow.createCell(index).setCellValue(header)
    }

    report.entities.foreach(addRow(sheet))

    (0 to headers.size) foreach sheet.autoSizeColumn
    workbook
  }

  val headers: Seq[String] = Seq(
    "First name",
    "Last name",
    "University ID",
    "Module",
    "Assignment",
    "Extension",
    "Working days late"
  )

  private def addRow(sheet: Sheet)(entity: MissedAssessmentsReportEntity): Unit = {
    val plainCellStyle = {
      val cs = sheet.getWorkbook.createCellStyle()
      cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
      cs
    }

    val row = sheet.createRow(sheet.getLastRowNum + 1)

    row.createCell(0).setCellValue(entity.student.firstName)
    row.createCell(1).setCellValue(entity.student.lastName)
    row.createCell(2).setCellValue(entity.student.universityId)
    row.getCell(2).setCellStyle(plainCellStyle)
    row.createCell(3).setCellValue(entity.module.code.toUpperCase())
    row.createCell(4).setCellValue(entity.assignment.name)
    row.createCell(5).setCellValue(entity.extension.flatMap(_.expiryDate).map(_.toString).getOrElse("None"))
    row.createCell(6).setCellValue(entity.workingDaysLate)
  }
}

class MissedAssessmentsReportXmlExporter(report: MissedAssessmentsReport) {
  def toXML: Elem = {
    <result>
      <students>
        {report.entities.groupBy(_.student).map { case (student, entities) =>
        <student
        firstname={student.firstName}
        lastname={student.lastName}
        universityId={student.universityId}>
          {entities.groupBy(_.module).map { case (module, moduleEntities) =>
          <module code={module.code}>
            {moduleEntities.map { entity =>
            <assignment
            id={entity.assignment.id}
            name={entity.assignment.name}
            closeDate={entity.assignment.closeDate.toString}>
              {Seq(entity.extension.map { ext =>
                <extension
                id={ext.id}
                expiryDate={ext.expiryDate.toString}/>
            }, entity.submission.map { sub =>
                <submission
                id={sub.id}
                date={sub.submittedDate.toString}
                late={sub.isLate.toString}/>
            }).flatten}
            </assignment>
          }}
          </module>
        }}
        </student>
      }}
      </students>
    </result>
  }
}
