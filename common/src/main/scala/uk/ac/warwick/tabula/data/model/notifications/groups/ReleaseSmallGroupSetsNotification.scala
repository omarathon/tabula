package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.{BatchedNotification, BatchedNotificationHandler, FreemarkerModel, MyWarwickActivity, Notification, UserIdRecipientNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

object ReleaseSmallGroupSetsNotification {
  val templateLocation = "/WEB-INF/freemarker/notifications/groups/release_small_group_notification.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("ReleaseSmallGroupSets")
class ReleaseSmallGroupSetsNotification
  extends BatchedNotification[SmallGroup, Unit, ReleaseSmallGroupSetsNotification](ReleaseSmallGroupSetsBatchedNotificationHandler)
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with MyWarwickActivity {

  def verb: String = "Release"

  override def onPreSave(newRecord: Boolean): Unit = {
    if (entities.isEmpty) {
      throw new IllegalArgumentException("Attempted to create a ReleaseSmallGroupSetsNotification with no SmallGroups!")
    }
  }

  def groups: Seq[SmallGroup] = entities

  def isStudent: Boolean = getBooleanSetting("isStudent", default = false)
  def isStudent_=(b: Boolean): Unit = {
    settings += ("isStudent" -> b)
  }

  // Delegate everything to the batched notification handler, this always acts like a batched notification anyway
  def title: String = ReleaseSmallGroupSetsBatchedNotificationHandler.titleForBatch(Seq(this), recipient)
  def content: FreemarkerModel = ReleaseSmallGroupSetsBatchedNotificationHandler.contentForBatch(Seq(this))
  def url: String = ReleaseSmallGroupSetsBatchedNotificationHandler.urlForBatch(Seq(this), recipient)
  def urlTitle: String = ReleaseSmallGroupSetsBatchedNotificationHandler.urlTitleForBatch(Seq(this))
}

object ReleaseSmallGroupSetsBatchedNotificationHandler extends BatchedNotificationHandler[ReleaseSmallGroupSetsNotification] {
  // The URL differs depending on whether student or tutor, so group them together
  override def groupBatchInternal(notifications: Seq[ReleaseSmallGroupSetsNotification]): Seq[Seq[ReleaseSmallGroupSetsNotification]] =
    notifications.groupBy(_.isStudent).values.toSeq

  private def formatsString(entities: Seq[SmallGroup]) = {
    val pluralFormats = entities.map(_.groupSet.format.plural.toLowerCase).distinct.toList
    pluralFormats match {
      case singleFormat :: Nil => singleFormat
      case _ => Seq(pluralFormats.init.mkString(", "), pluralFormats.last).mkString(" and ")
    }
  }

  override def titleForBatchInternal(notifications: Seq[ReleaseSmallGroupSetsNotification], user: User): String = {
    val entities = notifications.flatMap(_.entities)

    val moduleCodes = entities.map(_.groupSet.module.code.toUpperCase).distinct.toList.sorted
    val moduleCodesString = moduleCodes match {
      case singleModuleCode :: Nil => singleModuleCode
      case _ => Seq(moduleCodes.init.mkString(", "), moduleCodes.last).mkString(" and ")
    }

    "%s %s %s".format(moduleCodesString, formatsString(entities), if (entities.size == 1) "allocation" else "allocations")
  }

  override def contentForBatchInternal(notifications: Seq[ReleaseSmallGroupSetsNotification]): FreemarkerModel = {
    val recipient = notifications.head.recipient
    val groups = notifications.flatMap(_.groups)
    val url = urlForBatchInternal(notifications, recipient)

    FreemarkerModel(ReleaseSmallGroupSetsNotification.templateLocation, Map(
      "user" -> recipient,
      "groups" -> groups,
      "profileUrl" -> url
    ))
  }

  override def urlForBatchInternal(notifications: Seq[ReleaseSmallGroupSetsNotification], user: User): String =
    if (notifications.head.isStudent) {
      Routes.profiles.Profile.events(notifications.head.recipient.getWarwickId)
    } else {
      Routes.groups.tutor.mygroups
    }

  override def urlTitleForBatchInternal(notifications: Seq[ReleaseSmallGroupSetsNotification]): String =
    s"view your ${formatsString(notifications.flatMap(_.entities))} groups"
}
