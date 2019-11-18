package uk.ac.warwick.tabula.data.model.notifications.attendance

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.{Department, FreemarkerModel, MyWarwickNotification, NotificationWithTarget}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@DiscriminatorValue(value = "UnlinkedAttendanceMonitoringScheme")
class UnlinkedAttendanceMonitoringSchemeNotification extends NotificationWithTarget[AttendanceMonitoringScheme, Department]
  with MyWarwickNotification {

  @transient
  lazy val department: Department = target.entity
  @transient
  lazy val schemes: Seq[AttendanceMonitoringScheme] = items.asScala.toSeq.map(_.entity)
  @transient
  lazy val academicYear: AcademicYear = schemes.head.academicYear

  @transient
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
  @transient
  var topLevelUrl: String = Wire.property("${toplevel.url}")

  @transient
  override def verb: String = "view"

  override def urlTitle: String = "view the schemes in your department"

  @transient
  override def url: String = Routes.Manage.departmentForYear(target.entity, items.get(0).entity.academicYear)

  @transient
  override def title: String = "%s: %d monitoring %s been unlinked from SITS".format(
    department.name,
    schemes.size,
    if (schemes.size != 1) "schemes have" else "scheme has"
  )

  @transient
  override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unlinked.ftl", Map(
    "department" -> department,
    "academicYear" -> academicYear,
    "schemes" -> schemes,
    "schemeLinks" -> schemes.sortBy(_.displayName).map(scheme => s"${scheme.displayName}: $topLevelUrl${Routes.Manage.addStudentsToScheme(scheme)}")
  ))

  @transient
  override def recipients: Seq[User] =
  // department.owners is not populated correctly if department not fetched directly
    moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users.toSeq
}
