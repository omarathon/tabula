package uk.ac.warwick.tabula.api.commands.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order.asc
import uk.ac.warwick.tabula.JavaImports.{JInteger, JList}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.FiltersStudents
import uk.ac.warwick.tabula.data.{Aliasable, HibernateHelpers, ScalaRestriction}

trait UserSearchCommandRequest extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  var department: Department = _

  val defaultOrder = Seq(asc("lastName"), asc("firstName"))

  var sortOrder: JList[Order] = JArrayList()
  var courseTypes: JList[CourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var studyLevelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()
  var modules: JList[Module] = JArrayList()
  var hallsOfResidence: JList[String] = JArrayList()

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Profiles.ViewSearchResults, PermissionsTarget.Global)
  }

  def departmentRestriction: ScalaRestriction = Option(department).map(d =>
    Aliasable.addAliases(
      new ScalaRestriction(HibernateHelpers.is("studentCourseYearDetails.enrolmentDepartment", d)),
      FiltersStudents.AliasPaths("studentCourseYearDetails"):_*
    )).getOrElse(throw new IllegalArgumentException("Department not defined")
  )
}
