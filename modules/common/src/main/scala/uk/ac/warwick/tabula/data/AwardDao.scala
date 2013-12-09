package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository

import javax.persistence.{Entity, NamedQueries}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Award

trait AwardDaoComponent {
	val awardDao: AwardDao
}

trait AutowiringAwardDaoComponent extends AwardDaoComponent {
	val awardDao = Wire[AwardDao]
}

trait AwardDao {
	def saveOrUpdate(award: Award)
	def getByCode(code: String): Option[Award]
	def getAllAwardCodes: Seq[String]

}

@Repository
class AwardDaoImpl extends AwardDao with Daoisms {

	def saveOrUpdate(award: Award) = session.saveOrUpdate(award)

	def getByCode(code: String) = {
		val ret = session.newQuery[Award]("from Award award where code = :code").setString("code", code).uniqueResult
		ret
	}

	def getAllAwardCodes: Seq[String] =
		session.newQuery[String]("select distinct code from Award").seq

}
