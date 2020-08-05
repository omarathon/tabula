package uk.ac.warwick.tabula.data.model.notifications.marks

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.marks.web.{Routes => MarksRoutes}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User

object ConfirmedModuleMarkChangedNotification {
  val templateLocation: String = "/WEB-INF/freemarker/notifications/marks/module_confirmed_actualmark_changed.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("ConfirmModuleMarkChanged")
class ConfirmedModuleMarkChangedNotification
  extends BatchedNotificationWithTarget[RecordedModuleRegistration, Department, ConfirmedModuleMarkChangedNotification](ConfirmedModuleMarkChangedBatchedNotificationHandler)
    with SingleItemNotification[RecordedModuleRegistration]
    with MyWarwickNotification
    with Logging {

  @transient var profileService: ProfileService = Wire[ProfileService]
  @transient var topLevelUrl: String = Wire.property("${toplevel.url}")
  @transient lazy val department: Department = target.entity
  @transient lazy val recordedModuleRegistrations = entities

  override def onPreSave(isNew: Boolean): Unit = {
    priority = NotificationPriority.Info
  }

  def verb = "modified"

  def academicYear = recordedModuleRegistrations.head.academicYear

  def module = recordedModuleRegistrations.head.sitsModuleCode

  def studentList: Seq[StudentCourseYearDetails] = entities.flatMap { rmr =>
    profileService.getStudentCourseDetailsBySprCode(rmr.sprCode).filter(_.mostSignificant).flatMap(_.freshStudentCourseYearDetailsForYear(academicYear))
  }.sortBy(_.studentCourseDetails.scjCode)


  override def title: String = s"$module: Confirmed module marks have been changed"

  override def urlTitle = "view module marks in your department"

  override def url: String = MarksRoutes.Admin.home(department)

  override def content: FreemarkerModel = FreemarkerModel(ConfirmedModuleMarkChangedNotification.templateLocation, Map("memberLinks" -> studentList.map(scyd => s"${scyd.studentCourseDetails.student.universityId}: $topLevelUrl${Routes.Grids.assessmentdetails(scyd)}")))

  override def recipients: Seq[User] = department.owners.users.toSeq.filterNot(u => u == agent)

}

object ConfirmedModuleMarkChangedBatchedNotificationHandler extends BatchedNotificationHandler[ConfirmedModuleMarkChangedNotification] {
  override def groupBatchInternal(notifications: Seq[ConfirmedModuleMarkChangedNotification]): Seq[Seq[ConfirmedModuleMarkChangedNotification]] =
    // Batch notifications for modules in the same department
    notifications.groupBy(_.department).values.toSeq

  override def titleForBatchInternal(notifications: Seq[ConfirmedModuleMarkChangedNotification], user: User): String =
    "Confirmed module marks have been changed"

  // We can use the same template as the main notification, we just combine the students and add in the module code
  override def contentForBatchInternal(notifications: Seq[ConfirmedModuleMarkChangedNotification]): FreemarkerModel =
    FreemarkerModel(ConfirmedModuleMarkChangedNotification.templateLocation, Map(
      "memberLinks" -> notifications.groupBy(_.module).toSeq.sortBy(_._1).flatMap { case (moduleCode, notifications) =>
        notifications.flatMap(_.studentList).distinct.sortBy(_.studentCourseDetails.scjCode).map { scyd =>
          s"$moduleCode - ${scyd.studentCourseDetails.student.universityId}: ${notifications.head.topLevelUrl}${Routes.Grids.assessmentdetails(scyd)}"
        }
      }
    ))

  override def urlForBatchInternal(notifications: Seq[ConfirmedModuleMarkChangedNotification], user: User): String =
    notifications.head.url

  override def urlTitleForBatchInternal(notifications: Seq[ConfirmedModuleMarkChangedNotification]): String =
    notifications.head.urlTitle
}
