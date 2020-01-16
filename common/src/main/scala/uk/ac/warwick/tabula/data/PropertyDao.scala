package uk.ac.warwick.tabula.data

import scala.jdk.CollectionConverters._

import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.ModeOfAttendance

trait DecodeDao {
  def saveOrUpdate(modeOfAttendance: ModeOfAttendance): Unit

  def getByCode(code: String): Option[ModeOfAttendance]

  def getAllStatusCodes: Seq[String]

  def getFullName(code: String): Option[String]
}

