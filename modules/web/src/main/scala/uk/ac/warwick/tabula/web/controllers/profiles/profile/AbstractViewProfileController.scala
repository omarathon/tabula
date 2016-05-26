package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.profiles.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs.Profile.ProfileBreadcrumbIdentifier
import uk.ac.warwick.tabula.web.controllers.profiles.{ProfileBreadcrumbs, ProfilesController}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs => BaseBreadcumbs}

import scala.collection.JavaConverters._

abstract class AbstractViewProfileController extends ProfilesController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("siblingBreadcrumbs")
	def siblingBreadcrumbs = true

	protected def breadcrumbs(member: Member, activeIdentifier: ProfileBreadcrumbIdentifier): Seq[BreadCrumb] = Seq(
		ProfileBreadcrumbs.Profile.Identity(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Timetable(member).setActive(activeIdentifier)
	)

	protected def secondBreadcrumbs(activeAcademicYear: Option[AcademicYear], scd: StudentCourseDetails)(urlGenerator: (StudentCourseYearDetails) => String): Seq[BreadCrumb] = {
		val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails)
		if (scyds.isEmpty) {
			Seq()
		} else {
			val thisAcademicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
			val scydToSelect = scd.freshStudentCourseYearDetails.find(_.academicYear == thisAcademicYear).getOrElse(scd.freshStudentCourseYearDetails.last)
			scyds.map(scyd =>
				BaseBreadcumbs.Standard(
					"%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
					Some(urlGenerator(scyd)),
					"%s %s %s".format(scyd.studentCourseDetails.course.code, scyd.studentCourseDetails.currentRoute.code.toUpperCase, scyd.academicYear.getLabel)
				).setActive(scyd == scydToSelect)
			)
		}
	}

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@Autowired var conversionService: ConversionService = _

	@ModelAttribute("viewProfileCommand")
	protected def viewProfileCommand(@PathVariable pvs: JMap[String, String]) = {
		val pathVariables = pvs.asScala
		if (pathVariables.contains("member")) {
			new ViewProfileCommand(user, mandatory(
				conversionService.convert(pathVariables("member"), classOf[Member])
			))
		} else if (pathVariables.contains("studentCourseDetails")) {
			new ViewProfileCommand(user, mandatory(
				conversionService.convert(pathVariables("studentCourseDetails"), classOf[StudentCourseDetails])
			).student)
		}
	}


	@ModelAttribute("searchProfilesCommand")
	protected def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

}