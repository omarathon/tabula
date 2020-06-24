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

object ConfirmModuleMarkChangedNotification {
  val templateLocation: String = "/WEB-INF/freemarker/notifications/marks/module_confirmed_actualmark_changed.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("ConfirmModuleMarkChanged")
class ConfirmModuleMarkChangedNotification
  extends NotificationWithTarget[RecordedModuleRegistration, Department]
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


  override def title: String = s"$module: Student confirmed actual marks modified"

  override def urlTitle = "view module marks in your department"

  override def url: String = MarksRoutes.Admin.home(department)

  override def content: FreemarkerModel = FreemarkerModel(ConfirmModuleMarkChangedNotification.templateLocation, Map("memberLinks" -> studentList.map(scyd => s"${scyd.studentCourseDetails.student.universityId}: $topLevelUrl${Routes.Grids.assessmentdetails(scyd)}")))

  override def recipients: Seq[User] = department.owners.users.toSeq.filterNot(u => u == agent)

}
