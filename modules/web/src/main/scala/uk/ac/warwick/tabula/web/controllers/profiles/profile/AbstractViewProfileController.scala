package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.commands.profiles.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs.Profile.ProfileBreadcrumbIdentifier
import uk.ac.warwick.tabula.web.controllers.profiles.{ProfileBreadcrumbs, ProfilesController}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs => BaseBreadcumbs}

import scala.collection.JavaConverters._

abstract class AbstractViewProfileController extends ProfilesController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent
	with AutowiringAssessmentServiceComponent
	with RequestLevelCaching[String, Any] {

	@ModelAttribute("siblingBreadcrumbs")
	def siblingBreadcrumbs = true

	// Only do this once per request
	protected def relationshipTypesToDisplay(scd: StudentCourseDetails): Seq[StudentRelationshipType] =
		cachedBy(s"relationshipTypesToDisplay-${scd.id}") {
			relationshipService.allStudentRelationshipTypes.filter(relationshipType =>
				(scd.hasRelationship(relationshipType) ||
					relationshipType.displayIfEmpty(scd)) &&
					scd.isStudentRelationshipTypeForDisplay(relationshipType)
			)
		} match {
			case relationshipTypes: Seq[StudentRelationshipType]@unchecked => relationshipTypes
			case _ => throw new UnsupportedOperationException("Not a Seq[StudentRelationshipType]")
		}

	// Only do this once per request
	protected def scydToSelect(scd: StudentCourseDetails, activeAcademicYear: Option[AcademicYear]): Option[StudentCourseYearDetails] =
		cachedBy(s"scydToSelect-${scd.id}") {
			scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails) match {
				case Nil =>
					None
				case scyds =>
					val thisAcademicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
					Option(scd.freshStudentCourseYearDetails.find(_.academicYear == thisAcademicYear).getOrElse(scd.freshStudentCourseYearDetails.last))
			}
		} match {
			case scyd: Option[StudentCourseYearDetails]@unchecked => scyd
			case _ => throw new UnsupportedOperationException("Not a Option[StudentCourseYearDetails]")
		}

	protected def breadcrumbsStudent(
		activeAcademicYear: Option[AcademicYear],
		scd: StudentCourseDetails,
		activeIdentifier: ProfileBreadcrumbIdentifier
	): Seq[BreadCrumb] = {
		scydToSelect(scd, activeAcademicYear) match {
			case None =>
				breadcrumbsStaff(scd.student, activeIdentifier)
			case Some(scyd) =>
				Seq(
					ProfileBreadcrumbs.Profile.IdentityForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.TimetableForScyd(scyd).setActive(activeIdentifier)
				) ++ relationshipTypesToDisplay(scd).map(relationshipType =>
					ProfileBreadcrumbs.Profile.RelationshipTypeForScyd(scyd, relationshipType).setActive(activeIdentifier)
				) ++ Seq(
					ProfileBreadcrumbs.Profile.AssignmentsForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.ModulesForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.SeminarsForScyd(scyd).setActive(activeIdentifier),
					ProfileBreadcrumbs.Profile.AttendanceForScyd(scyd).setActive(activeIdentifier)
				) ++ (assessmentService.getAssignmentWhereMarker(MemberOrUser(scd.student).asUser) match {
					case Nil => Nil
					case Seq(assignments) => Seq(ProfileBreadcrumbs.Profile.MarkingForScyd(scyd).setActive(activeIdentifier))
				})
		}

	}

	protected def breadcrumbsStaff(member: Member, activeIdentifier: ProfileBreadcrumbIdentifier): Seq[BreadCrumb] = Seq(
		ProfileBreadcrumbs.Profile.Identity(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Timetable(member).setActive(activeIdentifier),
		ProfileBreadcrumbs.Profile.Marking(member).setActive(activeIdentifier)
	)

	protected def secondBreadcrumbs(activeAcademicYear: Option[AcademicYear], scd: StudentCourseDetails)(urlGenerator: (StudentCourseYearDetails) => String): Seq[BreadCrumb] = {
		scydToSelect(scd, activeAcademicYear).map(chooseScyd => {
			val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails)
			scyds.map(scyd =>
				BaseBreadcumbs.Standard(
					"%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
					Some(urlGenerator(scyd)),
					"%s %s %s".format(scyd.studentCourseDetails.course.code, scyd.studentCourseDetails.currentRoute.code.toUpperCase, scyd.academicYear.getLabel)
				).setActive(scyd == chooseScyd)
			)
		}).getOrElse(Nil)
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
		restricted(SearchProfilesCommand(currentMember, user)).orNull

}