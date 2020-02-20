package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventAttendanceNote

trait SmallGroupEventAttendanceNoteToJsonConverter {
  self: FileAttachmentToJsonConverter =>

  def jsonSmallGroupEventAttendanceNoteObject(note: SmallGroupEventAttendanceNote): Map[String, Any] = {
    Map(
      "absenceType" -> note.absenceType.dbValue,
      "contents" -> note.note,
      "updatedDate" -> DateFormats.IsoDateTime.print(note.updatedDate),
      "updatedBy" -> note.updatedBy,
      "attachment" -> Option(note.attachment).map(_.id).orNull,
    ) ++ (if (Option(note.attachment).nonEmpty) Map("attachment" -> jsonFileAttachmentObject(note.attachment)) else Map())
  }
}
