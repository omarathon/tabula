package uk.ac.warwick.tabula.data
import scala.collection.JavaConverters._

import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.ModeOfAttendance

trait ModeOfAttendanceDao {
	def saveOrUpdate(modeOfAttendance: ModeOfAttendance)
	def getByCode(code: String): Option[ModeOfAttendance]
	def getAllStatusCodes: Seq[String]
	def getFullName(code: String): Option[String]
}

@Repository
class ModeOfAttendanceDaoImpl extends ModeOfAttendanceDao with Daoisms {

	def saveOrUpdate(modeOfAttendance: ModeOfAttendance) = session.saveOrUpdate(modeOfAttendance)

	def getByCode(code: String) = 
		session.newQuery[ModeOfAttendance]("from ModeOfAttendance where code = :code").setString("code", code).uniqueResult

	def getAllStatusCodes: Seq[String] = 
		session.newQuery[String]("select distinct code from ModeOfAttendance").seq
	
	def getFullName(code: String): Option[String] =
		session.newQuery[String]("select fullName from ModeOfAttendance where code = :code").setString("code", code).uniqueResult
}
