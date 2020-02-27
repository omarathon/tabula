package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence.WeekNumber
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}

import scala.collection.{SortedMap, mutable}

trait SmallGroupAttendanceToJsonConverter {
  self: SmallGroupEventToJsonConverter with SmallGroupEventAttendanceNoteToJsonConverter =>

  def jsonSmallGroupEventWithWeekObject(eventWithWeek: (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)): (String, Map[String, Any]) = {
    eventWithWeek match {
      case (event, _) => event.id -> jsonSmallGroupEventObject(event)
    }
  }

  def jsonSmallGroupEventWithWeekRefObject(eventWithWeek: (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)): Map[String, Any] = {
    eventWithWeek match {
      case (event, week) => Map(
        "week" -> week,
        "id" -> event.id
      )
    }
  }

  def jsonSmallGroupEventAttendanceObject(attendance: SmallGroupEventAttendance): Map[String, Any] = {
    Map(
      "updatedDate" -> DateFormats.IsoDate.print(attendance.updatedDate),
      "updatedBy" -> attendance.updatedBy,
      "joinedOn" -> Option(attendance.joinedOn).map(DateFormats.IsoDate.print).orNull,
      "expectedToAttend" -> attendance.expectedToAttend,
      "addedManually" -> attendance.addedManually,
      "replacesAttendance" -> Option(attendance.replacesAttendance).map(replaced =>
        jsonSmallGroupEventWithWeekRefObject((replaced.occurrence.event, replaced.occurrence.week))).orNull,
      "replacedBy" -> attendance.replacedBy.asScala.map(jsonSmallGroupEventAttendanceObject)
    )
  }

  def jsonSmallGroupAttendanceObject(group: SmallGroupAttendanceInformation): Map[String, Any] = {
    val events = SortedMap(group.instances.map { case (event, _) =>
      event.id -> jsonSmallGroupEventObject(event)
    }: _*)
    val notes = group.notes.map { case (user, userNotes) =>
      user.getWarwickId -> userNotes.map { case (eventWithWeek, note) =>
        (eventWithWeek._1.id, eventWithWeek._2) -> jsonSmallGroupEventAttendanceNoteObject(note)
      }
    }
    val instances: mutable.Map[String, mutable.Set[WeekNumber]] = mutable.Map()

    group.instances.foreach { case (event, weekNumber) =>
      val set = instances.get(event.id)
      set match {
        case None => instances.addOne(event.id, mutable.Set(weekNumber))
        case Some(v) => v.addOne(weekNumber)
      }
    }

    Map(
      "events" -> events,
      "instances" -> instances,
      "attendance" -> group.attendance.map { case (user, events) =>
        Map(
          "student" -> user.getWarwickId,
          "events" -> events.map { case (eventWithWeek, attendanceStatus) => Map(
            "event" -> jsonSmallGroupEventWithWeekRefObject(eventWithWeek),
            "state" -> attendanceStatus._1.getName,
            "details" -> attendanceStatus._2.map(jsonSmallGroupEventAttendanceObject).orNull,
            "note" -> notes.get(user.getWarwickId).map(userNotes =>
              userNotes.get((eventWithWeek._1.id, eventWithWeek._2)).orNull).orNull
          )
          })
      }
    )
  }
}
