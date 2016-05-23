package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.joda.time.DateTime
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseYearDetails, StudentMember}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs.Profile.ProfileBreadcrumbIdentifier
import uk.ac.warwick.tabula.web.controllers.profiles.{ProfileBreadcrumbs, ProfilesController}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs => BaseBreadcumbs}

abstract class AbstractViewProfileController extends ProfilesController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("siblingBreadcrumbs")
	def siblingBreadcrumbs = true

	protected def breadcrumbs(member: Member, activeIdentifier: ProfileBreadcrumbIdentifier): Seq[BreadCrumb] = Seq(
		ProfileBreadcrumbs.Profile.Identity(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Timetable(member).setActive(activeIdentifier)
	)

	protected def secondBreadcrumbs(activeAcademicYear: Option[AcademicYear], student: StudentMember)(urlGenerator: (StudentCourseYearDetails) => String): Seq[BreadCrumb] = {
		val scyds = student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails)
		if (scyds.isEmpty) {
			Seq()
		} else {
			val thisAcademicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
			val academicYearToSelect = scyds.find(_.academicYear == thisAcademicYear) match {
				case Some(scyd) => scyd.academicYear
				case _ => scyds.last.academicYear
			}
			scyds.map(scyd =>
				BaseBreadcumbs.Standard(
					"%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
					Some(urlGenerator(scyd)),
					"%s %s %s".format(scyd.studentCourseDetails.course.code, scyd.studentCourseDetails.currentRoute.code.toUpperCase, scyd.academicYear.getLabel)
				).setActive(scyd.academicYear.startYear == academicYearToSelect.startYear)
			)
		}
	}

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}