package uk.ac.warwick.tabula.services.cm2.docconversion

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import java.io.InputStream

import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.XSSFComment
import uk.ac.warwick.tabula.data.model.forms.FormField
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkerFeedback}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

import scala.util.Try

class MarkItem extends AutowiringUserLookupComponent {
	var id: String = _
	var actualMark: String = _
	var actualGrade: String = _
	var fieldValues: JMap[String, String] = JHashMap()
	var stage: Option[MarkingWorkflowStage] = None
	var isValid = true
	var isModified = false

	def user(assignment:Assignment): Option[User] = id.maybeText.map(userLookup.getUserByWarwickUniId).filter(u => u.isFoundUser && !u.isLoginDisabled)
		.orElse(id.maybeText.map(userLookup.getUserByWarwickUniIdUncached(_, skipMemberLookup = true)).filter(u => u.isFoundUser && !u.isLoginDisabled))
		.orElse(id.maybeText.map(userLookup.getUserByUserId).filter(u => u.isFoundUser && !u.isLoginDisabled))
		.orElse({
			val anonId = id.maybeText.flatMap { asStr => Try(asStr.toInt).toOption }
			anonId.flatMap(id => assignment.allFeedback.find(_.anonymousId.contains(id)).map(f => userLookup.getUserByUserId(f.usercode)))
		})

	def currentFeedback(assignment: Assignment): Option[Feedback] = for {
		u <- user(assignment)
		f <- assignment.allFeedback.find(_.usercode == u.getUserId)
	} yield f

	def currentMarkerFeedback(assignment: Assignment, marker: User): Option[MarkerFeedback] = for {
		f <- currentFeedback(assignment)
		cmf <- f.markerFeedback.asScala.find(mf => marker == mf.marker && f.outstandingStages.asScala.contains(mf.stage))
	} yield cmf

}

@Service
class MarksExtractor {
	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of MarkItems
	 */
	def readXSSFExcelFile(assignment:Assignment, file: InputStream): JList[MarkItem] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val markItems: JList[MarkItem] = JArrayList()
		val sheetHandler = MarkItemXslxSheetHandler(styles, sst, markItems, assignment)
		val parser = sheetHandler.fetchSheetParser
		for (sheet <- reader.getSheetsData.asScala) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		markItems.asScala.filterNot(markItem => markItem.id == null && markItem.actualMark == null && markItem.actualGrade == null)
	}.asJava
}

trait MarksExtractorComponent {
	val marksExtractor: MarksExtractor
}

trait AutowiringMarksExtractorComponent extends MarksExtractorComponent {
	val marksExtractor: MarksExtractor = Wire[MarksExtractor]
}

object MarkItemXslxSheetHandler {
	def apply(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[MarkItem], assignment: Assignment) =
		new MarkItemXslxSheetHandler(styles, sst, markItems, assignment)
}

class MarkItemXslxSheetHandler(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[MarkItem], assignment: Assignment)
	extends AbstractXslxSheetHandler(styles, sst, markItems) with SheetContentsHandler with Logging {

	override def newCurrentItem = new MarkItem()

	override def cell(cellReference: String, formattedValue: String, comment: XSSFComment){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow){
			columnMap(col) = formattedValue
		} else if (columnMap.asJava.containsKey(col)) {
			columnMap(col) match {
				case "University ID" | "ID" =>
					currentItem.id = formattedValue
				case "Mark" =>
					if(formattedValue.hasText)
						currentItem.actualMark = formattedValue
				case "Grade" =>
					currentItem.actualGrade = formattedValue
				case label if assignment.feedbackFields.exists(_.label == label) =>
					currentItem.fieldValues.put(assignment.feedbackFields.find(_.label == label).map(_.name).get, formattedValue)
				case _ => // ignore anything else
			}
		}
	}

}
