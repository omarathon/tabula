package uk.ac.warwick.tabula.web.controllers.exams.exams.admin


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/new"))
class AddExamController extends ExamsController {
  @RequestMapping(method = Array(HEAD, GET))
  def showForm(@PathVariable module: Module, @PathVariable academicYear: AcademicYear): Mav = {
    Mav("exams/exams/admin/new",
      "department" -> module.adminDepartment
    ).crumbs(
      Breadcrumbs.Exams.Home(academicYear),
      Breadcrumbs.Exams.Department(module.adminDepartment, academicYear),
      Breadcrumbs.Exams.Module(module, academicYear)
    )
  }
}
