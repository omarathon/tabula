package uk.ac.warwick.tabula.services.mitcircs

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseDetailsDaoComponent, StudentCourseDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.{Department, Route, StudentMember}
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesOfficerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}

import scala.annotation.tailrec
import scala.collection.immutable.TreeMap

trait MitCircsReportService {
  def studentsUnableToSubmit: Seq[StudentsUnableToSubmitForDepartment]
}

abstract class AbstractMitCircsReportService extends MitCircsReportService with TaskBenchmarking  {
  self: StudentCourseDetailsDaoComponent with PermissionsServiceComponent with SecurityServiceComponent =>

  val noRoute: Route = new Route("none", null) { name = "No route" }

  def canSubmitMitCircs(d: Department): Boolean = RequestLevelCache.cachedBy("MitCircsReportService.canSubmitMitCircs", d) {

    @tailrec
    def hasMco(d: Department): Boolean = {
      if (d.parent == null) !permissionsService.ensureUserGroupFor(d, MitigatingCircumstancesOfficerRoleDefinition).isEmpty
      else !permissionsService.ensureUserGroupFor(d, MitigatingCircumstancesOfficerRoleDefinition).isEmpty || hasMco(d.parent)
    }

    d.enableMitCircs && hasMco(d)
  }

  def studentsUnableToSubmit: Seq[StudentsUnableToSubmitForDepartment] = benchmark("studentsUnableToSubmitMitcircs"){
    studentCourseDetailsDao.getCurrentStudents
      .filter(_.department != null)
      .filter(scd => !scd.department.subDepartmentsContaining(scd.student).exists(canSubmitMitCircs))
      .groupBy(_.department)
      .view.mapValues(s => TreeMap(s.groupBy(s => Option(s.currentRoute).getOrElse(noRoute)).view.mapValues(_.map(_.student)).toSeq:_*)).toSeq
      .map{ case (d, s) => StudentsUnableToSubmitForDepartment(d, s) }
      .sortBy(_.department.code)
  }
}

case class StudentsUnableToSubmitForDepartment(
  department: Department,
  studentsByRoute: TreeMap[Route, Seq[StudentMember]]
) {
  def totalStudents: Int = studentsByRoute.values.map(_.size).sum
}

@Service("mitCircsReportService")
class AutowiredMitCircsReportService extends AbstractMitCircsReportService
  with AutowiringStudentCourseDetailsDaoComponent
  with AutowiringPermissionsServiceComponent
  with AutowiringSecurityServiceComponent

trait MitCircsReportServiceComponent {
  def mitCircsReportService: MitCircsReportService
}

trait AutowiringMitCircsReportServiceComponent extends MitCircsReportServiceComponent {
  var mitCircsReportService: MitCircsReportService = Wire[MitCircsReportService]
}
