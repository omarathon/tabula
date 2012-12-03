package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty

import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType._
import org.hibernate.annotations.Type
import org.joda.time.LocalDate

import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.actions.Viewable
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.userlookup.User

@Entity
class Member extends Viewable with ToString {
	@Id @BeanProperty var universityId: String = _
	@BeanProperty @Column(nullable = false) var userId: String = _
	@BeanProperty var firstName: String = _
	@BeanProperty var lastName: String = _
	@BeanProperty var email: String = _
	
	@BeanProperty var title: String = _
	@BeanProperty var fullFirstName: String = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.GenderUserType")
	@BeanProperty var gender: Gender = _
	
	@BeanProperty var nationality: String = _
	@BeanProperty var homeEmail: String = _
	@BeanProperty var mobileNumber: String = _
	
	@OneToOne
	@JoinColumn(name="PHOTO_ID")
	@BeanProperty var photo: FileAttachment = _
	
	@BeanProperty var inUseFlag: String = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var inactivationDate: LocalDate = _
	
	@BeanProperty var groupName: String = _
	
	@ManyToOne
	@JoinColumn(name = "home_department_id")
	@BeanProperty var homeDepartment: Department = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var dateOfBirth: LocalDate = _
	
	@BeanProperty var teachingStaff: JBoolean = _
	@BeanProperty var sprCode: String = _
	@BeanProperty var sitsCourseCode: String = _
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	@BeanProperty var route: Route = _
	
	@BeanProperty var yearOfStudy: JInteger = _
	@BeanProperty var attendanceMode: String = _
	
	@BeanProperty var studentStatus: String = _
	
	@BeanProperty var fundingSource: String = _
	@BeanProperty var programmeOfStudy: String = _
	
	@BeanProperty var intendedAward: String = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var academicYear: AcademicYear = _
	
	@ManyToOne
	@JoinColumn(name = "study_department_id")
	@BeanProperty var studyDepartment: Department = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var courseStartYear: AcademicYear = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var yearCommencedDegree: AcademicYear = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var courseBaseYear: AcademicYear = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var courseEndDate: LocalDate = _
	
	@BeanProperty var transferReason: String = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var beginDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var endDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var expectedEndDate: LocalDate = _
	
	@BeanProperty var feeStatus: String = _
	@BeanProperty var domicile: String = _
	@BeanProperty var highestQualificationOnEntry: String = _
	
	@BeanProperty var lastInstitute: String = _
	@BeanProperty var lastSchool: String = _
	
	@OneToOne
	@Cascade(Array(SAVE_UPDATE, DETACH))
	@JoinColumn(name="HOME_ADDRESS_ID")
	@BeanProperty var homeAddress: Address = null
	
	@OneToOne
	@Cascade(Array(SAVE_UPDATE, DETACH))
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@BeanProperty var termtimeAddress: Address = null

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, orphanRemoval=true)
	@Cascade(Array(SAVE_UPDATE, DETACH))
	@BeanProperty var nextOfKins:JList[NextOfKin] = ArrayList()
	
	@BeanProperty def fullName = firstName + " " + lastName
	@BeanProperty def officialName = title + " " + fullFirstName + " " + lastName

	def asSsoUser = {
		val u = new User
		u.setUserId(userId)
		u.setWarwickId(universityId)
		u.setFirstName(firstName)
		u.setLastName(lastName)
		u.setFullName(fullName)
		u.setEmail(email)
		u.setDepartment(homeDepartment.name)
		u.setDepartmentCode(homeDepartment.code)
		u.setFoundUser(true)
		u
	}
	
	def toStringProps = Seq(
		"universityId" -> universityId,
		"userId" -> userId,
		"name" -> (firstName + " " + lastName),
		"email" -> email)

}