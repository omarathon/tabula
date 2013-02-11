package uk.ac.warwick.tabula.coursework.services.docconversion

import org.apache.poi.xssf.model.StylesTable
import org.xml.sax.helpers.XMLReaderFactory
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFSheetXMLHandler}
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import uk.ac.warwick.util.core.StringUtils

class XslxSheetHandler(var styles: StylesTable, var sst: ReadOnlySharedStringsTable, var markItems: JList[MarkItem])
	extends SheetContentsHandler with Logging {

	var lastContents: String = null
	var cellIsString = false
	var isFirstRow = true // flag to skip the first row as it will contain column headers
	var columnMap = scala.collection.mutable.Map[Short, String]()
	var columnIndex: Int = _
	var currentMarkItem: MarkItem = _

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
		if (row > 0) {
			isFirstRow = false
			currentMarkItem = new MarkItem()
		}
	}

	override def cell(cellReference: String, formattedValue: String){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow){
			columnMap(col) = formattedValue
		} else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "ID" => {
					currentMarkItem.universityId = formattedValue
				}
				case "Mark" => {
					if(StringUtils.hasText(formattedValue))
						currentMarkItem.actualMark = formattedValue
				}
				case "Grade" => {
					currentMarkItem.actualGrade = formattedValue
				}
				case _ => // ignore anything else
			}
		}
	}

	override def endRow() {
		if (!isFirstRow)
			markItems.add(currentMarkItem)
	}
}