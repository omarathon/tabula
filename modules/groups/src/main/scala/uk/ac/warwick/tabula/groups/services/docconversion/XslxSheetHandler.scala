package uk.ac.warwick.tabula.groups.services.docconversion


import org.apache.poi.xssf.model.StylesTable
import org.xml.sax.helpers.XMLReaderFactory
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFSheetXMLHandler}
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.UniversityId

class XslxSheetHandler(var styles: StylesTable, var sst: ReadOnlySharedStringsTable, var allocateStudentItems: JList[AllocateStudentItem])
	extends SheetContentsHandler with Logging {

	var isFirstRow = true // flag to skip the first row as it will contain column headers
	var foundStudentInRow = false
	
	var columnMap = scala.collection.mutable.Map[Short, String]()
	var currentAllocateStudentItem: AllocateStudentItem = _

	val xssfHandler = new XSSFSheetXMLHandler(styles, sst, this, false)

	def fetchSheetParser = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		parser.setContentHandler(xssfHandler)
		parser
	}

	// don't care about handling this, but required for interface
	override def headerFooter(text: String, isHeader: Boolean, tagName: String){}

	override def startRow(row: Int){
		logger.debug("startRow: " + row.toString)
		
		isFirstRow = (row == 0)
		currentAllocateStudentItem = new AllocateStudentItem()
		foundStudentInRow = false
	}

	override def cell(cellReference: String, formattedValue: String){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow) columnMap(col) = formattedValue
		else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "student_id" => {
					if (formattedValue.hasText) {
						currentAllocateStudentItem.universityId = UniversityId.zeroPad(formattedValue)
						foundStudentInRow = true
					}
				}
				case "group_id" => {
					if (formattedValue.hasText)
						currentAllocateStudentItem.groupId = formattedValue
				}
				case _ => // ignore anything else
			}
		}
	}

	override def endRow() {
		if (!isFirstRow && foundStudentInRow) allocateStudentItems.add(currentAllocateStudentItem)
	}
}

