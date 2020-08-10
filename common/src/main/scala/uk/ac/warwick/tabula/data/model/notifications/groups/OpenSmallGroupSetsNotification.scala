package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

object OpenSmallGroupSetsNotification {
  @transient val templateLocation = "/WEB-INF/freemarker/notifications/groups/open_small_group_student_notification.ftl"
}

abstract class AbstractOpenSmallGroupSetsNotification
  extends BatchedNotification[SmallGroupSet, Unit, AbstractOpenSmallGroupSetsNotification](OpenSmallGroupSetsBatchedNotificationHandler)
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with MyWarwickActivity {

  def verb = "Opened"

  // Delegate everything to the batched notification handler, this always acts like a batched notification anyway
  def title: String = OpenSmallGroupSetsBatchedNotificationHandler.titleForBatch(Seq(this), null)
  def content: FreemarkerModel = OpenSmallGroupSetsBatchedNotificationHandler.contentForBatch(Seq(this))
  def url: String = OpenSmallGroupSetsBatchedNotificationHandler.urlForBatch(Seq(this), null)
  def urlTitle: String = OpenSmallGroupSetsBatchedNotificationHandler.urlTitleForBatch(Seq(this))
}

@Entity
@Proxy
@DiscriminatorValue(value = "OpenSmallGroupSets")
class OpenSmallGroupSetsStudentSignUpNotification
  extends AbstractOpenSmallGroupSetsNotification with AllCompletedActionRequiredNotification {

  priority = Warning

}

@Entity
@Proxy
@DiscriminatorValue(value = "OpenSmallGroupSetsOtherSignUp")
class OpenSmallGroupSetsOtherSignUpNotification extends AbstractOpenSmallGroupSetsNotification

object OpenSmallGroupSetsBatchedNotificationHandler extends BatchedNotificationHandler[AbstractOpenSmallGroupSetsNotification] {
  private def formatsString(entities: Seq[SmallGroupSet]) = {
    val pluralFormats = entities.map(_.format.plural.toLowerCase).distinct.toList
    pluralFormats match {
      case singleFormat :: Nil => singleFormat
      case _ => Seq(pluralFormats.init.mkString(", "), pluralFormats.last).mkString(" and ")
    }
  }

  override def titleForBatchInternal(notifications: Seq[AbstractOpenSmallGroupSetsNotification], user: User): String = {
    val entities = notifications.flatMap(_.entities)

    val moduleCodes = entities.map(_.module.code.toUpperCase).distinct.toList.sorted
    val moduleCodesString = moduleCodes match {
      case singleModuleCode :: Nil => singleModuleCode
      case _ => Seq(moduleCodes.init.mkString(", "), moduleCodes.last).mkString(" and ")
    }

    val pluralFormatsString = formatsString(entities)

    "%s %s are now open for sign up".format(moduleCodesString, pluralFormatsString)
  }

  override def contentForBatchInternal(notifications: Seq[AbstractOpenSmallGroupSetsNotification]): FreemarkerModel = {
    val entities = notifications.flatMap(_.entities)

    FreemarkerModel(OpenSmallGroupSetsNotification.templateLocation, Map(
      "groupsets" -> entities,
      "profileUrl" -> Routes.groups.home,
      "formatsString" -> formatsString(entities)
    ))
  }

  override def urlForBatchInternal(notifications: Seq[AbstractOpenSmallGroupSetsNotification], user: User): String =
    Routes.groups.home

  override def urlTitleForBatchInternal(notifications: Seq[AbstractOpenSmallGroupSetsNotification]): String =
    s"sign up for these ${formatsString(notifications.flatMap(_.entities))} groups"
}
