package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.data.model.mitcircs.IssueType
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, PersistenceTestBase}
import uk.ac.warwick.userlookup.User

class MitCircsSubmissionDaoTest extends PersistenceTestBase {

  val mitCircsSubmissionDao = new MitCircsSubmissionDaoImpl

  @Before
  def setup() {
    mitCircsSubmissionDao.sessionFactory = sessionFactory
  }

  val mockUserLookup = new MockUserLookup
  mockUserLookup.registerUserObjects(new User("cusfal"), new User("cuscao"))


  @Test def fetchByIdAndKey(): Unit = transactional { tx =>
    val heronReason = """My hatred of herons is consuming me.
      I spend all my waking moments obsessing over their cold reptilian eyes and foul dank plumage instead of focusing on my studies."""
    val s = Fixtures.mitigatingCircumstancesSubmission("cuslaj", "1431777")
    s.issueType = IssueType.Deterioration
    s.reason = heronReason

    mitCircsSubmissionDao.saveOrUpdate(s)

    val byId = mitCircsSubmissionDao.getById(s.id)
    byId.isDefined should be (true)
    byId.get.issueType should be (IssueType.Deterioration)
    byId.get.reason should be (heronReason)

    val byKey = mitCircsSubmissionDao.getByKey(s.key)
    byKey.isDefined should be (true)
    byKey.get.issueType should be (IssueType.Deterioration)
    byKey.get.reason should be (heronReason)
  }

  @Test def keysAreSequential(): Unit = transactional { tx =>
    val a = Fixtures.mitigatingCircumstancesSubmission("cuslaj", "1431777")
    mitCircsSubmissionDao.saveOrUpdate(a)
    val b = Fixtures.mitigatingCircumstancesSubmission("cuslaj", "1431778")
    mitCircsSubmissionDao.saveOrUpdate(b)
    val c = Fixtures.mitigatingCircumstancesSubmission("cuslaj", "1431779")
    mitCircsSubmissionDao.saveOrUpdate(c)

    b.key == a.key + 1 && c.key == b.key +1 should be (true)
  }
}
