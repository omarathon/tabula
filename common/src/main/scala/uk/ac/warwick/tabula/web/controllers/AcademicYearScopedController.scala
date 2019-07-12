package uk.ac.warwick.tabula.web.controllers

import java.time.{LocalDate, Month}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.{MaintenanceModeServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs}
import uk.ac.warwick.tabula.{AcademicYear, Features}

trait AcademicYearScopedController extends TaskBenchmarking {

  self: BaseController with UserSettingsServiceComponent with MaintenanceModeServiceComponent =>

  @Autowired var features: Features = _

  @ModelAttribute("availableAcademicYears")
  def availableAcademicYears: Seq[AcademicYear] = benchmarkTask("availableAcademicYears") {
    AcademicYear(2012).to {
      if (LocalDate.now().getMonth.getValue >= Month.JUNE.getValue) {
        AcademicYear.now().next
      } else {
        AcademicYear.now()
      }
    }
  }

  protected def retrieveActiveAcademicYear(academicYearOption: Option[AcademicYear]): Option[AcademicYear] = {
    academicYearOption match {
      case Some(academicYear) if maintenanceModeService.enabled =>
        // Don't store if maintenance mode is enabled
        Some(academicYear)
      case Some(academicYear) if user.apparentUser.isFoundUser && !ajax =>
        // Store the new active academic year and return it
        val settings = new UserSettings(user.apparentId)
        settings.activeAcademicYear = academicYear
        transactional() {
          userSettingsService.save(user, settings)
        }
        Some(academicYear)
      case Some(academicYear) => Some(academicYear) // just return the academic year if there is no user to save to
      case _ =>
        userSettingsService.getByUserId(user.apparentId).flatMap(_.activeAcademicYear)
    }
  }

  @ModelAttribute("academicYearNow")
  def academicYearNow: AcademicYear = AcademicYear.now()

  /**
    * One of these should be overriden to just call retrieveActiveAcademicYear,
    * but with the PathVariable-provided academic year as an argument (or null),
    * and annotated with @ModelAttribute("activeAcademicYear").
    */
  def activeAcademicYear(academicYear: AcademicYear): Option[AcademicYear] = {
    None
  }

  def activeAcademicYear: Option[AcademicYear] = {
    None
  }

  def academicYearBreadcrumbs(activeAcademicYear: AcademicYear)(urlGenerator: AcademicYear => String): Seq[BreadCrumb] =
    availableAcademicYears.map(year =>
      Breadcrumbs.AcademicYearScoped(
        title = year.getLabel,
        url = Some(urlGenerator(year)),
        tooltip = "",
        scopedAcademicYear = Some(year),
      ).setActive(year.startYear == activeAcademicYear.startYear)
    )
}
