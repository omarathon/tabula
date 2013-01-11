package uk.ac.warwick.tabula.profiles.services.docconversion

import java.lang.Override

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener
import org.apache.poi.hssf.eventusermodel.HSSFListener
import org.apache.poi.hssf.record.CellRecord
import org.apache.poi.hssf.record.LabelSSTRecord
import org.apache.poi.hssf.record.NumberRecord
import org.apache.poi.hssf.record.Record
import org.apache.poi.hssf.record.RowRecord
import org.apache.poi.hssf.record.SSTRecord

import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.helpers.Logging

class XslSheetHandler extends HSSFListener with Logging {

	val memberRelationships: JList[RawMemberRelationship] = ArrayList();
	var sstrec: SSTRecord = null
	val formatListener = new FormatTrackingHSSFListener(this)

	@Override
	def processRecord(record: Record) = {
		record match {
			case record: SSTRecord => {
				sstrec = record
			}
			case record: RowRecord => {
				memberRelationships.add(new RawMemberRelationship)
			}
			case record: CellRecord => {
				processCell(record, sstrec)
			}
			case _ =>
		}
	}

	def processCell(record: CellRecord, currentSST: SSTRecord) {
		val rowNumber = record.getRow
		val currentRawMemberRelationship = memberRelationships.get(rowNumber)
		val cellValue = record match {
			case record: LabelSSTRecord => currentSST.getString(record.getSSTIndex()).toString()
			case record: NumberRecord => record.getValue.toInt toString
		}

		record.getColumn match {
			case 0 => currentRawMemberRelationship.subjectUniversityId = cellValue
			case 1 => currentRawMemberRelationship.agentUniversityId = cellValue
			case 2 => currentRawMemberRelationship.agentName = cellValue
			case _ =>
		}
	}
}