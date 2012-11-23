package uk.ac.warwick.tabula.coursework.services.docconversion

import org.apache.poi.xssf.model.SharedStringsTable
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging

class XslxSheetHandler extends DefaultHandler with Logging {

	var sst: SharedStringsTable = _
	var markItems: JList[MarkItem] = _
	var lastContents: String = null
	var cellIsString = false
	var isFirstRow = true // flag to skip the first row as it will contain column headers
	var columnIndex: Int = _
	var currentMarkItem: MarkItem = _

	def this(sst: SharedStringsTable, markItems: JList[MarkItem]) = {
		this();
		this.sst = sst;
		this.markItems = markItems;
	}

	override def startElement(uri: String, localName: String, name: String, attributes: Attributes) = {
		name match {
			case "row" => { // row
				//reset column index
				columnIndex = 0
				currentMarkItem = new MarkItem()
			}
			case "c" => { // cell
				columnIndex = columnIndex + 1
				val cellType = attributes.getValue("t");
				cellIsString = (cellType != null && cellType.equals("s"))
				// Clear contents cache
				lastContents = "";
			}
			case _ =>
		}
	}

	override def endElement(uri: String, localName: String, name: String) = {
		name match {
			case "c" => {
				if (!isFirstRow) {
					// retrieve strings from the shared strings table as required	
					if (cellIsString) {
						val idx = Integer.parseInt(lastContents);
						lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
						cellIsString = false;
					}
					columnIndex match {
						case 1 => currentMarkItem.universityId = lastContents
						case 2 => currentMarkItem.actualMark = lastContents
						case 3 => currentMarkItem.actualGrade = lastContents
						case _ =>
					}
				}
			}
			case "row" => {
				if (isFirstRow)
					isFirstRow = false
				else
					markItems.add(currentMarkItem)
			}
			case _ =>
		}
	}

	override def characters(ch: Array[Char], start: Int, length: Int) = {
		lastContents += new String(ch, start, length);
	}
}