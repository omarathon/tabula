package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.data.model.StudentMember
import scala.collection.JavaConverters._


trait MembersForPointSet extends ProfileServiceComponent {

	def getMembers(pointSet: MonitoringPointSet): Seq[StudentMember] = {
		if(pointSet.year == null) {
			profileService.getStudentsByRoute(
				pointSet.route,
				pointSet.academicYear
			).toStream.distinct
		} else {
			profileService.getStudentsByRoute(
				pointSet.route,
				pointSet.academicYear
			).filter{
				student => student.studentCourseDetails.asScala.exists{
					scd => scd.studentCourseYearDetails.asScala.exists{
						scyd =>
							scyd.yearOfStudy == pointSet.year && scyd.academicYear == pointSet.academicYear
					}
				}
			}
		}
	}
}
