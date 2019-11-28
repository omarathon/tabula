package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.ModeOfAttendance

trait ModeOfAttendanceDaoComponent {
  val modeOfAttendanceDao: ModeOfAttendanceDao
}

trait AutowiringModeOfAttendanceDaoComponent extends ModeOfAttendanceDaoComponent {
  val modeOfAttendanceDao: ModeOfAttendanceDao = Wire[ModeOfAttendanceDao]
}

trait ModeOfAttendanceDao {
  def saveOrUpdate(modeOfAttendance: ModeOfAttendance)

  def getByCode(code: String): Option[ModeOfAttendance]

  def getAllStatusCodes: Seq[String]

  def getAll: Seq[ModeOfAttendance]

  def getFullName(code: String): Option[String]
}

@Repository
class ModeOfAttendanceDaoImpl extends ModeOfAttendanceDao with Daoisms {

  def saveOrUpdate(modeOfAttendance: ModeOfAttendance): Unit = session.saveOrUpdate(modeOfAttendance)

  def getByCode(code: String): Option[ModeOfAttendance] =
    session.newQuery[ModeOfAttendance]("from ModeOfAttendance where code = :code").setString("code", code).uniqueResult

  def getAllStatusCodes: Seq[String] =
    session.newQuery[String]("select distinct code from ModeOfAttendance").seq

  def getAll: Seq[ModeOfAttendance] =
    session.newCriteria[ModeOfAttendance].seq

  def getFullName(code: String): Option[String] =
    session.newQuery[String]("select fullName from ModeOfAttendance where code = :code").setString("code", code).uniqueResult
}
