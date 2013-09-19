package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}
import org.junit.Before

class MemberNoteDaoTest extends PersistenceTestBase {

	val memberNoteDao = new MemberNoteDaoImpl

	@Before
	def setup() {
		  memberNoteDao.sessionFactory = sessionFactory
	}

	@Test def saveAndFetch {
		transactional { tx =>

			val student = Fixtures.student("123", "abc")
			val memberNote = Fixtures.memberNote("the note", student, "123")

			memberNoteDao.getById(memberNote.id) should be (None)

			memberNoteDao.saveOrUpdate(memberNote)

			memberNoteDao.getById(memberNote.id) should be (Option(memberNote))
			memberNoteDao.getById(memberNote.id).get.note should be ("the note")

		}
	}

}
