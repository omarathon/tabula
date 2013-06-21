package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.{FreemarkerTestHelpers, TestBase}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.groups.{SmallGroupEventBuilder, SmallGroupFixture}
import uk.ac.warwick.tabula.JavaImports.JHashMap
import org.junit.Test
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalTime

class SmallGroupSetChangedNotificationTemplateTest extends TestBase with FreemarkerTestHelpers with FreemarkerRendering{

    private trait NotificationFixture extends SmallGroupFixture {

      val weekRangeFormatter = new StubFreemarkerMethodModel
      val urlModel = new StubFreemarkerDirectiveModel
      val timeBuilder= new StubFreemarkerMethodModel

      // make group1 have 2 events (the default fixture only has 1) so we can verify iteration over
      // the group's events
      val event = new SmallGroupEventBuilder()
        .withDay(DayOfWeek.Monday)
        .withStartTime(new LocalTime(12,0,0,0))
        .withLocation("TEST").build
      event.group = group1
      group1.events.add(event)


      implicit val config = newFreemarkerConfiguration(JHashMap(
        "url" -> urlModel,
        "weekRangesFormatter" -> weekRangeFormatter,
        "timeBuilder"->timeBuilder))
    }

  @Test
  def includesTheNameOfTheGroup{
    new NotificationFixture {
      val output =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
          Map("student" -> recipient, "group" -> group1, "profileUrl" -> "profileUrl"))
      output should include(group1.name)
    }}

  @Test
  def includesTheCountOfStudents{new NotificationFixture {
    val output =
      renderToString(SmallGroupSetChangedNotification.templateLocation,
        Map("student" -> recipient, "group" -> group1, "profileUrl" -> "profileUrl"))
    output should include("2 students")
  }}

  @Test
  def rendersProfileUrlOnceOnly{new NotificationFixture {
    val output =
      renderToString(SmallGroupSetChangedNotification.templateLocation,
        Map("student" -> recipient, "group" -> group1, "profileUrl" -> "profileUrl"))
    verify(urlModel.mockDirective, times(1)).execute(anyObject(),anyMap,anyObject(),anyObject())
  }}

  @Test
  def callsWeekRangeFormatterForEachEvent() {
    new NotificationFixture {
      val output =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
          Map("student" -> recipient, "group" -> group1, "profileUrl" -> "profileUrl"))
      verify(weekRangeFormatter.mock, times(2)).exec(anyList())
    }}

  @Test
  def formatsTimeNicelyForEachEvent(){
    new NotificationFixture {
      val output =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
          Map("student" -> recipient, "group" -> group1, "profileUrl" -> "profileUrl"))
      verify(timeBuilder.mock, times(2)).exec(anyList())
    }}

  }
