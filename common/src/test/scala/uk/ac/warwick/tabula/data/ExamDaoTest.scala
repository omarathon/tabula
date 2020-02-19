package uk.ac.warwick.tabula.data

import org.hibernate.Session
import uk.ac.warwick.tabula.data.model.{ExamQuestion, ExamQuestionRule}
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}

class ExamDaoTest extends PersistenceTestBase {

  val dao: ExamDaoImpl with ExtendedSessionComponent = new ExamDaoImpl with ExtendedSessionComponent {
    override def session: Session = ExamDaoTest.this.session
  }

  @Test def crud(): Unit = transactional { tx =>
    val exam = Fixtures.exam()
    val question = new ExamQuestion(exam, "1")
    val child = Fixtures.examQuestion(exam, question, "a", 5)
    val child2 = Fixtures.examQuestion(exam, question, "b", 5)

    dao.save(exam)
    session.flush()

    val rule = new ExamQuestionRule(exam, Seq(child, child2), min=1, max=1)
    dao.save(exam)
    session.flush()

    val fetchedExam = dao.getById(exam.id)
    fetchedExam should be (Some(exam))
    val e = fetchedExam.get
    e.name should be("Summer exam")
    e.questions.size should be(3)
    e.rules.size should be (1)
  }

}