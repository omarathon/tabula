package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventAttendanceNote

trait SmallGroupEventAttendanceNoteToJsonConverter {
  self: FileAttachmentToJsonConverter =>

  def jsonSmallGroupEventAttendanceNoteObject(note: SmallGroupEventAttendanceNote): Map[String, Any] = {
    Map(
      "absenceType" -> note.absenceType.dbValue,
      "absenceTypeDescription" -> note.absenceType.description,
      "contents" -> note.note,
      "updatedDate" -> DateFormats.IsoDateTime.print(note.updatedDate),
      "updatedBy" -> note.updatedBy
    ) ++ (if (Option(note.attachment).nonEmpty) Map("attachment" -> jsonFileAttachmentObject(note.attachment)) else Map())
  }
}
