package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import java.io.ByteArrayOutputStream

import javax.activation.DataHandler
import javax.mail.Part
import javax.mail.internet.MimeBodyPart
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property._
import org.hibernate.ObjectNotFoundException
import org.springframework.mail.javamail.MimeMessageHelper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.HasSettings._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.timetables.{EventOccurrenceService, TermBasedEventOccurrenceService}
import uk.ac.warwick.tabula.timetables.TimetableEvent

abstract class ScheduledMeetingRecordNotification
  extends Notification[ScheduledMeetingRecord, Unit]
    with SingleItemNotification[ScheduledMeetingRecord] {

  self: MyWarwickDiscriminator =>

  def meeting: ScheduledMeetingRecord = try {
    item.entity
  } catch {
    case _: ClassCastException => throw new ObjectNotFoundException("", "")
  }

  def verbSetting: StringSetting = StringSetting("verb", "")

  def verb: String = verbSetting.value

  def academicYear: AcademicYear = AcademicYear.forDate(meeting.meetingDate)

  def url: String = Routes.Profile.relationshipType(
    meeting.relationships.head.studentCourseDetails,
    academicYear,
    meeting.relationships.head.relationshipType
  )

  def urlTitle = "view this meeting"

  def agentRoles: Seq[String] = meeting.relationshipTypes.map(_.agentRole)
}

trait AddsIcalAttachmentToScheduledMeetingNotification extends HasNotificationAttachment {

  self: ScheduledMeetingRecordNotification =>

  @transient
  lazy val eventOccurrenceService: EventOccurrenceService = Wire[TermBasedEventOccurrenceService]

  override def generateAttachments(message: MimeMessageHelper): Unit = {
    // Create ical
    val cal = new Calendar
    cal.getProperties.add(Version.VERSION_2_0)
    cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
    cal.getProperties.add(CalScale.GREGORIAN)
    cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT12H"))

    val vEvent = eventOccurrenceService.toVEvent(
      item.entity.toEventOccurrence(TimetableEvent.Context.Staff).get
    )

    verb match {
      case "deleted" =>
        cal.getProperties.add(Method.CANCEL)
        vEvent.getProperties.add(Status.VEVENT_CANCELLED)
        vEvent.getProperties.add(new Sequence(1))
      case "rescheduled" | "updated" =>
        cal.getProperties.add(Method.PUBLISH)
        vEvent.getProperties.add(Status.VEVENT_CONFIRMED)
        vEvent.getProperties.add(new Sequence(1))
      case _ =>
        cal.getProperties.add(Method.PUBLISH)
        vEvent.getProperties.add(Status.VEVENT_CONFIRMED)
        vEvent.getProperties.add(new Sequence(0))
    }

    cal.getComponents.add(vEvent)

    val outputter = new CalendarOutputter
    val baos = new ByteArrayOutputStream
    outputter.output(cal, baos)
    baos.close()
    val iCal = baos.toByteArray

    // iCal part for Outlook/Office 365
    val icalPart = new MimeBodyPart
    icalPart.setDataHandler(new DataHandler(ByteArrayDataSource(iCal, s"text/calendar; method=${cal.getMethod.getValue}", "meeting.ics")))
    message.getRootMimeMultipart.addBodyPart(icalPart)

    // Attachment for everyone else
    message.addAttachment("meeting.ics", ByteArrayDataSource(iCal, "text/calendar", "meeting.ics"))
  }

}

