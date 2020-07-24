package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Classification

trait ClassificationDaoComponent {
  val classificationDao: ClassificationDao
}

trait AutowiringClassificationDaoComponent extends ClassificationDaoComponent {
  val classificationDao: ClassificationDao = Wire[ClassificationDao]
}

trait ClassificationDao {
  def saveOrUpdate(Classification: Classification): Unit

  def getByCode(code: String): Option[Classification]

  def getAllClassificationCodes: Seq[String]

  def getAll: Seq[Classification]

}

@Repository
class ClassificationDaoImpl extends ClassificationDao with Daoisms {

  def saveOrUpdate(Classification: Classification): Unit = session.saveOrUpdate(Classification)

  def getByCode(code: String): Option[Classification] =
    session.newQuery[Classification]("from Classification Classification where code = :code").setString("code", code).uniqueResult

  def getAllClassificationCodes: Seq[String] =
    session.newQuery[String]("select distinct code from Classification").seq

  def getAll: Seq[Classification] = session.newCriteria[Classification].seq

}
