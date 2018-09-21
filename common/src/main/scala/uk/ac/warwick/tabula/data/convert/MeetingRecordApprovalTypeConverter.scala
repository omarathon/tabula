package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.MeetingRecordApprovalType
import uk.ac.warwick.tabula.system.TwoWayConverter

class MeetingRecordApprovalTypeConverter extends TwoWayConverter[String, MeetingRecordApprovalType] {
	override def convertRight(source: String): MeetingRecordApprovalType = MeetingRecordApprovalType.fromCode(source).orNull

	override def convertLeft(source: MeetingRecordApprovalType): String = source.code
}
