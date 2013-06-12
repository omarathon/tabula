package uk.ac.warwick.tabula.groups.services.docconversion

import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.JavaImports._
import java.io.InputStream

class AllocateStudentItem {

	var universityId: String = _
	var groupId: String = _

	def this(universityId: String, groupId: String) = {
		this()
		this.universityId = universityId
		this.groupId = groupId
	}
}

@Service
class GroupsExtractor {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of AllocateStudentItem
	 */
	def readXSSFExcelFile(file: InputStream): JList[AllocateStudentItem] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val allocateStudentItems: JList[AllocateStudentItem] = JArrayList()
		val sheetHandler = new XslxSheetHandler(styles, sst, allocateStudentItems)
		val parser = sheetHandler.fetchSheetParser

		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		allocateStudentItems
	}
}




