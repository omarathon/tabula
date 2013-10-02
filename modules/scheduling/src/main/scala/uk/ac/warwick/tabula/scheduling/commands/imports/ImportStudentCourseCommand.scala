package uk.ac.warwick.tabula.scheduling.commands.imports
import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.PrsCode
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentCourseProperties
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.PrsCode
import uk.ac.warwick.tabula.data.model.StudentCourseProperties
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.CourseAndRouteService
import java.sql.BatchUpdateException
import org.hibernate.exception.ConstraintViolationException
import uk.ac.warwick.tabula.scheduling.services.CourseImporter
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.StudentRelationshipSource
import uk.ac.warwick.tabula.data.model.CourseType

class ImportStudentCourseCommand(resultSet: ResultSet,
		importStudentCourseYearCommand: ImportStudentCourseYearCommand,
		importSupervisorsForStudentCommand: ImportSupervisorsForStudentCommand)
	extends Command[StudentCourseDetails] with Logging with Daoisms
	with StudentCourseProperties with Unaudited with PropertyCopying {

	import ImportMemberHelpers._

	implicit val rs = resultSet

	var memberDao = Wire.auto[MemberDao]
	var relationshipService = Wire.auto[RelationshipService]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]
	var courseAndRouteService = Wire.auto[CourseAndRouteService]
	var courseImporter = Wire.auto[CourseImporter]

	// Grab various codes from the result set into local variables ready to persist as objects
	var routeCode = rs.getString("route_code")
	var courseCode = rs.getString("course_code")
	var sprStatusCode = rs.getString("spr_status_code")
	var departmentCode = rs.getString("department_code")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	var sprTutor1 = rs.getString("spr_tutor1")

	// This needs to be assigned before apply is called.
	// (can't be in the constructor because it's not yet created then)
	// TODO - use promises to make sure it gets assigned
	var stuMem: StudentMember = _

	// this is the key and is not included in StudentCourseProperties, so just storing it in a var:
	var scjCode: String = rs.getString("scj_code")

	// now grab data from the result set into properties
	this.mostSignificant = rs.getString("most_signif_indicator") match {
		case "Y" | "y" => true
		case _ => false
	}

	this.sprCode = rs.getString("spr_code")
	this.awardCode = rs.getString("award_code")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))
	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))
	this.courseYearLength = rs.getString("course_year_length")
	this.levelCode = rs.getString("level_code")

	override def applyInternal(): StudentCourseDetails = transactional() {
		val studentCourseDetailsExisting = studentCourseDetailsDao.getByScjCode(scjCode)

		logger.debug("Importing student course details for " + scjCode)

		val (isTransient, studentCourseDetails) = studentCourseDetailsExisting match {
			case Some(studentCourseDetails: StudentCourseDetails) => (false, studentCourseDetails)
			case _ => (true, new StudentCourseDetails(stuMem, scjCode))
		}

		val commandBean = new BeanWrapperImpl(this)
		val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

		val hasChanged = copyStudentCourseProperties(commandBean, studentCourseDetailsBean)

		if (isTransient || hasChanged) {
			try {
				logger.debug("Saving changes for " + studentCourseDetails)

				studentCourseDetails.lastUpdatedDate = DateTime.now
				studentCourseDetailsDao.saveOrUpdate(studentCourseDetails)
			}
			catch  {
				case exception: ConstraintViolationException => {
					logger.warn("Couldn't update course details for SCJ "
							+ studentCourseDetails.scjCode + ", SPR " + studentCourseDetails.sprCode
							+ ".  Might be invalid data in SITS - working on the assumption "
							+ "there shouldn't be multiple SPR codes for one current SCJ code")
					exception.printStackTrace
				}
			}
		}

		importStudentCourseYearCommand.studentCourseDetails = studentCourseDetails
		val studentCourseYearDetails = importStudentCourseYearCommand.apply()

		// Apply above will take care of the db.  This brings the in-memory data up to speed:
		studentCourseDetails.attachStudentCourseYearDetails(studentCourseYearDetails)

		captureTutor(studentCourseDetails.department)

		importSupervisorsForStudentCommand.studentCourseDetails = studentCourseDetails
		importSupervisorsForStudentCommand.apply

		studentCourseDetails
	}

	private val basicStudentCourseProperties = Set(
		"sprCode",
		"scjCode",
		"awardCode",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"courseYearLength",
		"mostSignificant",
		"levelCode"
	)

	private def copyStudentCourseProperties(commandBean: BeanWrapper, studentCourseDetailsBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseProperties, commandBean, studentCourseDetailsBean) |
		copyObjectProperty("department", departmentCode, studentCourseDetailsBean, toDepartment(departmentCode)) |
		copyObjectProperty("route", routeCode, studentCourseDetailsBean, toRoute(routeCode)) |
		copyObjectProperty("course", courseCode, studentCourseDetailsBean, toCourse(courseCode)) |
		copyObjectProperty("sprStatus", sprStatusCode, studentCourseDetailsBean, toSitsStatus(sprStatusCode))
	}

	private def toRoute(routeCode: String) = {
		if (routeCode == null || routeCode == "") {
			null
		} else {
			courseAndRouteService.getRouteByCode(routeCode.toLowerCase).getOrElse(null)
		}
	}

	def toCourse(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			courseImporter.getCourseForCode(code).getOrElse(null)
		}
	}

	def captureTutor(dept: Department) = {

		if (dept == null)
			logger.warn("Trying to capture tutor for " + sprCode + " but department is null.")

		// Mark Hadley in Physics says "I don't think the University uses the term 'tutor' for PGRs"
		// so by default excluding PGRs from the personal tutor import:
		else if (courseCode != null && courseCode.length() > 0 && CourseType.fromCourseCode(courseCode) != CourseType.PGR)
			// is this student in a department that is set to import tutor data from SITS?
			relationshipService
				.getStudentRelationshipTypeByUrlPart("tutor") // TODO this is awful
				.filter { relType => dept.getStudentRelationshipSource(relType) == StudentRelationshipSource.SITS }
				.foreach { relationshipType =>
					val tutorUniIdOption = PrsCode.getUniversityId(sprTutor1)

					tutorUniIdOption match {
						case Some(tutorUniId: String) => {
							// only save the personal tutor if we can match the ID with a staff member in Tabula
							val member = memberDao.getByUniversityId(tutorUniId) match {
								case Some(mem: Member) => {
									logger.info("Got a personal tutor from SITS! SprCode: " + sprCode + ", tutorUniId: " + tutorUniId)

									val currentRelationships = relationshipService.findCurrentRelationships(relationshipType, sprCode)

									// Does this relationship already exist?
									currentRelationships.find(_.agent == tutorUniId) match {
										case Some(existing) => existing
										case _ => {
											// End all existing relationships
											currentRelationships.foreach { rel =>
												rel.endDate = DateTime.now
												relationshipService.saveOrUpdate(rel)
											}

											// Save the new one
											val rel = relationshipService.saveStudentRelationship(relationshipType, sprCode, tutorUniId)

											rel
										}
									}
								}
								case _ => {
									logger.warn("SPR code: " + sprCode + ": no staff member found for PRS code " + sprTutor1 + " - not importing this personal tutor from SITS")
								}
							}
						}
						case _ => logger.warn("Can't parse PRS code " + sprTutor1 + " for student " + sprCode)
					}
				}
		}
	}

