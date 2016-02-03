package uk.ac.warwick.tabula.services.coursework.docconversion

import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConversions._

class MarkItemXslxSheetHandler(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[MarkItem])
	extends AbstractXslxSheetHandler(styles, sst, markItems) with SheetContentsHandler with Logging {

	override def newCurrentItem = new MarkItem()

	override def cell(cellReference: String, formattedValue: String){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow){
			columnMap(col) = formattedValue
		} else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "University ID" | "ID" =>
					currentItem.universityId = formattedValue
				case "Mark" =>
					if(formattedValue.hasText)
						currentItem.actualMark = formattedValue
				case "Grade" =>
					currentItem.actualGrade = formattedValue
				case _ => // ignore anything else
			}
		}
	}

}