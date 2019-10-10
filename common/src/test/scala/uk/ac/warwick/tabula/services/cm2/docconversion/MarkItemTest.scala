package uk.ac.warwick.tabula.services.cm2.docconversion

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{FeedbackService, UserLookupService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

import scala.collection.JavaConverters._

class MarkItemTest extends TestBase with Mockito {

  val assignment: Assignment = Fixtures.assignment("Essay")
  assignment.feedbackService = smartMock[FeedbackService]
  assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _ => assignment.feedbacks.asScala }

  private def markItem(): MarkItem = {
    val item = new MarkItem
    item.userLookup = smartMock[UserLookupService]
    item
  }

  @Test
  def userEmpty(): Unit = {
    val item = markItem()
    item.user(assignment) should be('empty)
  }

  @Test
  def userFromBackend(): Unit = {
    val item = markItem()
    item.id = "1234567"

    val user = Fixtures.user(universityId = "1234567")
    item.userLookup.getUserByWarwickUniId("1234567") returns user

    item.user(assignment) should be(Some(user))
  }

  @Test
  def userAsUsercode(): Unit = {
    val item = markItem()
    item.id = "u1234567"

    item.userLookup.getUserByWarwickUniId("u1234567") returns new AnonymousUser
    item.userLookup.getUserByWarwickUniId("u1234567", true) returns new AnonymousUser

    val user = Fixtures.user(userId = "u1234567")
    item.userLookup.getUserByUserId("u1234567") returns user

    item.user(assignment) should be(Some(user))
  }

  @Test
  def userAnonymousId(): Unit = {
    val item = markItem()
    item.id = "999"

    item.userLookup.getUserByWarwickUniId("999") returns new AnonymousUser
    item.userLookup.getUserByWarwickUniId("999", true) returns new AnonymousUser
    item.userLookup.getUserByUserId("999") returns new AnonymousUser

    val feedback = Fixtures.assignmentFeedback(userId = "u1234567")
    feedback.anonymousId = Some(999)
    assignment.feedbacks.add(feedback)

    val user = Fixtures.user(userId = "u1234567")
    item.userLookup.getUserByUserId("u1234567") returns user

    item.user(assignment) should be(Some(user))
  }

  @Test
  def userNotFound(): Unit = {
    val item = markItem()
    item.id = "u1234567"

    item.userLookup.getUserByWarwickUniId("u1234567") returns new AnonymousUser
    item.userLookup.getUserByWarwickUniId("u1234567", true) returns new AnonymousUser
    item.userLookup.getUserByUserId("u1234567") returns new AnonymousUser

    item.user(assignment) should be('empty)
  }

}
