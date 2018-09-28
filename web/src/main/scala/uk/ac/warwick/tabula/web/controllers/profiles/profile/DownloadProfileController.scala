package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.reports.profiles.ProfileExportSingleCommand
import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipFileItem}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class DownloadProfileController extends AbstractViewProfileController with AutowiringZipServiceComponent {
	def command(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Option[Appliable[Seq[FileAttachment]]] =
		restricted(ProfileExportSingleCommand(studentCourseDetails.student, academicYear, user))

	@GetMapping(Array("/{member}/download"))
	def formByMember(@PathVariable student: StudentMember, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav =
		form(student.mostSignificantCourse, activeAcademicYear)

	@GetMapping(Array("/{studentCourseDetails}/{academicYear}/download"))
	def formByCourse(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear): Mav =
		form(studentCourseDetails, Some(mandatory(academicYear)))

	private def form(studentCourseDetails: StudentCourseDetails, activeAcademicYear: Option[AcademicYear]) = {
		val thisAcademicYear = scydToSelect(studentCourseDetails, activeAcademicYear).map(_.academicYear).getOrElse(AcademicYear.now())

		Mav("profiles/profile/download",
			"hasPermission" -> command(studentCourseDetails, thisAcademicYear).nonEmpty,
			"member" -> studentCourseDetails.student
		).crumbs(breadcrumbsStudent(activeAcademicYear, studentCourseDetails, ProfileBreadcrumbs.Profile.DownloadIdentifier): _*)
			.secondCrumbs(secondBreadcrumbs(activeAcademicYear, studentCourseDetails)(scyd => Routes.Profile.download(scyd)): _*)
	}

	@PostMapping(Array("/{studentCourseDetails}/{academicYear}.zip"))
	def download(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear): RenderableFile = {
		val fileAttachments = mandatory(command(studentCourseDetails, academicYear)).apply()

		zipService.createUnnamedZip(fileAttachments.zipWithIndex.map { case (a, index) =>
			ZipFileItem.apply(if (index == 0) a.name else s"${a.id}-${a.name}", a.asByteSource, a.actualDataLength)
		})
	}
}
