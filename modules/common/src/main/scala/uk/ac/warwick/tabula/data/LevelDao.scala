package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.StudyLevel
import uk.ac.warwick.spring.Wire

trait LevelDaoComponent {
	val levelDao: LevelDao
}

trait AutowiringLevelDaoComponent extends LevelDaoComponent {
	val levelDao = Wire[LevelDao]
}

trait LevelDao {
	def saveOrUpdate(level: StudyLevel)
	def getByCode(code: String): Option[StudyLevel]
	def getAllLevelCodes: Seq[String]

}

@Repository
class LevelDaoImpl extends LevelDao with Daoisms {

	def saveOrUpdate(level: StudyLevel) = session.saveOrUpdate(level)

	def getByCode(code: String) = {
		val ret = session.newQuery[StudyLevel]("from StudyLevel level where code = :code").setString("code", code).uniqueResult
		ret
	}

	def getAllLevelCodes: Seq[String] =
		session.newQuery[String]("select distinct code from StudyLevel").seq

}
