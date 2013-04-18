package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._

import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.SitsStatus

trait SitsStatusDao {
	def saveOrUpdate(sitsStatus: SitsStatus)
	def getByCode(code: String): Option[SitsStatus]
	def getAllStatusCodes: Seq[String]
	def getFullName(code: String): Option[String]
}

@Repository
class SitsStatusDaoImpl extends SitsStatusDao with Daoisms {

	def saveOrUpdate(sitsStatus: SitsStatus) = session.saveOrUpdate(sitsStatus)

	def getByCode(code: String) = 
		session.newQuery[SitsStatus]("from SitsStatus sitsStatus where code = :code").setString("code", code).uniqueResult

	def getAllStatusCodes: Seq[String] = 
		session.newQuery[String]("select distinct code from SitsStatus").seq
	
	def getFullName(code: String): Option[String] =
		session.newQuery[String]("select fullName from SitsStatus where code = :code").setString("code", code).uniqueResult
}
