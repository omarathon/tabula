package uk.ac.warwick.tabula.data.model.notifications.groups

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.{Module, Notification}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroup, SmallGroupFormat, SmallGroupSet, WeekRange}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.helpers.{TimeBuilder, WeekRangesFormatterTag}
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, MarkdownRendererImpl, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, SmallGroupEventBuilder, SmallGroupFixture, TestBase}
import uk.ac.warwick.userlookup.User

class ReleaseSmallGroupSetsNotificationTest extends TestBase with Mockito with FreemarkerRendering with MarkdownRendererImpl {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  freeMarkerConfig.setSharedVariable("timeBuilder", new TimeBuilder)

  val weekRangesFormatterTag = new WeekRangesFormatterTag
  weekRangesFormatterTag.userSettingsService = smartMock[UserSettingsService]
  weekRangesFormatterTag.userSettingsService.getByUserId(anyString) returns None
  freeMarkerConfig.setSharedVariable("weekRangesFormatter", weekRangesFormatterTag)

  val module1: Module = Fixtures.module("cs118")
  val module2: Module = Fixtures.module("cs119")
  val module3: Module = Fixtures.module("cs120")

  val set1: SmallGroupSet = Fixtures.smallGroupSet("set 1")
  set1.format = SmallGroupFormat.Seminar
  set1.module = module1

  val set2: SmallGroupSet = Fixtures.smallGroupSet("set 2")
  set2.format = SmallGroupFormat.Seminar
  set2.module = module1

  val set3: SmallGroupSet = Fixtures.smallGroupSet("set 3")
  set3.format = SmallGroupFormat.Seminar
  set3.module = module1

  val group1: SmallGroup = Fixtures.smallGroup("group 1")
  group1.groupSet = set1

  val group2: SmallGroup = Fixtures.smallGroup("group 2")
  group2.groupSet = set2

  val group3: SmallGroup = Fixtures.smallGroup("group 3")
  group3.groupSet = set3

  @Test def title(): Unit = withUser("cuscav", "0672089") {
    val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, group1)
    notification.userLookup = new MockUserLookup
    notification.title should be("CS118 seminars allocation")
  }

  @Test def titlePlural(): Unit = withUser("cuscav", "0672089") {
    val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, Seq(group1, group2, group3))
    notification.userLookup = new MockUserLookup
    notification.title should be("CS118 seminars allocations")

    set2.module = module3
    set2.format = SmallGroupFormat.Workshop
    notification.title should be("CS118 and CS120 seminars and workshops allocations")

    set3.module = module2
    set2.format = SmallGroupFormat.Example
    notification.title should be("CS118, CS119 and CS120 seminars and example classes allocations")
  }

  val TEST_CONTENT = "test"

  def createNotification(group: SmallGroup, actor: User, recipient: User, isStudent: Boolean = true): ReleaseSmallGroupSetsNotification =
    createMultiGroupNotification(Seq(group), actor, recipient, isStudent)

  def createMultiGroupNotification(groups: Seq[SmallGroup], actor: User, recipient: User, isStudent: Boolean = true): ReleaseSmallGroupSetsNotification = {
    val n = Notification.init(new ReleaseSmallGroupSetsNotification, actor, groups)
    n.recipientUserId = recipient.getUserId
    n.isStudent = isStudent
    n
  }

  @Test
  def titleIncludesGroupFormat(): Unit = {
    new SmallGroupFixture {
      val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient)
      n.userLookup = userLookup
      n.title should be("LA101 labs allocation")
    }
  }

  @Test
  def titleJoinsMultipleGroupSetsNicely(): Unit = {
    new SmallGroupFixture {
      val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Seq(group1, group2, group3), actor, recipient)
      n.userLookup = userLookup
      n.title should be("LA101, LA102 and LA103 labs, seminars and tutorials allocations")
    }
  }

  @Test
  def titleRemovesDuplicateFormats(): Unit = {
    new SmallGroupFixture {
      val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Seq(group1, group2, group3, group4, group5), actor, recipient)
      n.userLookup = userLookup
      n.title should be("LA101, LA102, LA103, LA104 and LA105 labs, seminars and tutorials allocations")
    }
  }

  @Test(expected = classOf[IllegalArgumentException])
  def cantCreateANotificationWithNoGroups(): Unit = {
    new SmallGroupFixture {
      val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Nil, actor, recipient)
      n.preSave(true)
    }
  }

  @Test
  def urlIsProfilePageForStudents(): Unit = new SmallGroupFixture {
    val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient, isStudent = true)
    n.userLookup = userLookup
    n.url should be(s"/profiles/view/${recipient.getWarwickId}/events")

  }

  @Test
  def urlIsMyGroupsPageForTutors(): Unit = new SmallGroupFixture {
    val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient, isStudent = false)
    n.userLookup = userLookup
    n.url should be(Routes.tutor.mygroups)
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = new SmallGroupFixture {
    val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient)
    n.userLookup = userLookup
    n.content.template should be("/WEB-INF/freemarker/notifications/groups/release_small_group_notification.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new SmallGroupFixture {
    val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient)
    n.userLookup = userLookup
    n.content.model.get("user") should be(Some(recipient))
    n.content.model.get("profileUrl") should be(Some(s"/profiles/view/${recipient.getWarwickId}/events"))
    n.content.model.get("groups") should be(Some(List(group1)))
  }

  @Test
  def content(): Unit = new SmallGroupFixture {
    // Add a second event for group1 with no tutors
    private val group1ExtraEvent =
      new SmallGroupEventBuilder()
        .withWeekRange(WeekRange(3, 4))
        .withDay(DayOfWeek.Tuesday)
        .withStartTime(new LocalTime(15, 0))
        .build

    group1.addEvent(group1ExtraEvent)
    group1ExtraEvent.group = group1

    // Add a second event for group2 that's unscheduled
    private val group2ExtraEvent =
      new SmallGroupEventBuilder()
        .withTutors(createUserGroup(Seq(tutor1.getUserId, tutor2.getUserId), identifierIsUniNumber = false))
        .build

    group2.addEvent(group2ExtraEvent)
    group2ExtraEvent.group = group2

    // Add a second event for group3 that's unscheduled and has no tutors (so shouldn't show up)
    private val group3ExtraEvent =
      new SmallGroupEventBuilder().build

    group3.addEvent(group3ExtraEvent)
    group3ExtraEvent.group = group3

    val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Seq(group1, group2, group3, group4, group5), actor, recipient)
    n.userLookup = userLookup

    private val content = n.content
    private val contentAsString = renderToString(freeMarkerConfig.getTemplate(content.template), content.model)
    contentAsString should be (
      """You have been allocated the following small teaching groups:
        |
        |- LA101 A Groupset 1 small group 1, 2 students
        |    - 12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2
        |    - 15:00 Tuesday, Term 1, weeks 3-4
        |- LA102 A Groupset 2 small group 2, 2 students
        |    - (unscheduled event), Tutors: Tutor Number1, Tutor Number2
        |    - 12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2
        |- LA103 A Groupset 3 small group 3, 2 students
        |    - (unscheduled event)
        |    - 12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2
        |- LA104 A Groupset 4 small group 4, 2 students
        |    - 12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2
        |- LA105 A Groupset 5 small group 5, 2 students
        |    - 12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2
        |""".stripMargin
    )

    // Make sure the events are in a nested list
    renderMarkdown(contentAsString) should be (
      """<p>You have been allocated the following small teaching groups:</p>
        |<ul><li>LA101 A Groupset 1 small group 1, 2 students
        |<ul><li>12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2</li><li>15:00 Tuesday, Term 1, weeks 3-4</li></ul>
        |</li><li>LA102 A Groupset 2 small group 2, 2 students
        |<ul><li>(unscheduled event), Tutors: Tutor Number1, Tutor Number2</li><li>12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2</li></ul>
        |</li><li>LA103 A Groupset 3 small group 3, 2 students
        |<ul><li>(unscheduled event)</li><li>12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2</li></ul>
        |</li><li>LA104 A Groupset 4 small group 4, 2 students
        |<ul><li>12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2</li></ul>
        |</li><li>LA105 A Groupset 5 small group 5, 2 students
        |<ul><li>12:00 Monday, CMR0.1, Term 1, weeks 1-10, Tutors: Tutor Number1, Tutor Number2</li></ul>
        |</li></ul>
        |""".stripMargin
    )
  }

}
