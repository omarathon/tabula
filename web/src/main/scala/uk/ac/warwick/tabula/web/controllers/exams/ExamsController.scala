package uk.ac.warwick.tabula.web.controllers.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember, StudentCourseDetails, StudentCourseYearDetails}

import uk.ac.warwick.tabula.web.controllers.{BaseController, CurrentMemberComponent}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs => BaseBreadcumbs}

abstract class ExamsController extends BaseController with ExamsBreadcrumbs with CurrentMemberComponent {
  final def optionalCurrentMember: Option[Member] = user.profile

  final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)
}

trait StudentCourseYearDetailsBreadcrumbs {
  def scydBreadcrumbs(activeAcademicYear: AcademicYear, scd: StudentCourseDetails)(urlGenerator: StudentCourseYearDetails => String): Seq[BreadCrumb] = {
    val chooseScyd = scd.freshStudentCourseYearDetailsForYear(activeAcademicYear) // fresh scyd for this year
      .orElse(scd.freshOrStaleStudentCourseYearDetailsForYear(activeAcademicYear))
      .getOrElse(throw new UnsupportedOperationException("Not valid StudentCourseYearDetails for given academic year"))

    val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails) match {
      case Nil =>
        scd.student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
      case fresh =>
        fresh
    }
    scyds.map(scyd =>
      BaseBreadcumbs.Standard(
        title = "%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
        url = Some(urlGenerator(scyd)),
        tooltip = "%s %s".format(
          scyd.studentCourseDetails.course.name,
          scyd.academicYear.getLabel
        )
      ).setActive(scyd == chooseScyd)
    ).toSeq
  }
}
