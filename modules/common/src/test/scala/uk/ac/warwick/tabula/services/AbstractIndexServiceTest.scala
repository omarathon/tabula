package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import org.joda.time.DateTime
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer
import java.io.File
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import scala.util.Random
import org.apache.lucene.search._
import org.apache.lucene.index.Term
import org.apache.lucene.search.SearcherLifetimeManager.Pruner

/** Overrides the default file-based directory with an in-memory one.
	* Faster than the disk one, plus data is entirely scoped to the instance of
	* RAMDirectory so no extra cleanup required.
	*/
trait RAMDirectoryOverride { self: OpensLuceneDirectory =>
	var indexPath: File = null // unused
	private val directory = new RAMDirectory()
	protected override def openDirectory() = directory
}

class AbstractIndexServiceTest extends TestBase {
	case class Item(name: String, date: DateTime)

	val fakeItems = for (i <- 1 to 100) yield Item(s"item$i", new DateTime().plusMinutes(i))
	val service = new MockIndexService()

	val dateRangeQuery = NumericRangeQuery.newLongRange("date", DateTime.now.minusYears(2).getMillis, null, true, true)

	@Test
	def newest() {
		indexFakeItems()
		val newest = service.newest().getOrElse( fail("Newest not found!") )
		newest.get("name") should be ("item100")
		newest.get("date").toLong should be (fakeItems.last.date.getMillis)
	}

	// Paging that passes the DocID of the last item on the previous page. Also passes a token
	// to try to reuse an existing IndexSearcher if possible.
	@Test
	def docBasedPaging() {
		indexFakeItems()
		val query = dateRangeQuery
		val page1 = service.search(query, 5, service.reverseDateSort, None, None)
		val page1Results = toItems(page1.results)
		page1Results.map{ _.name } should be ((96 to 100).reverse.map{ i => s"item$i"} )

		// Clear the searcher to simulate the searcher not being found; instance restarted,
		// or second request went to a different instance, or searcher timed out.
		service.clearSearcher(page1.token)

		val page2 = service.search(query, 5, service.reverseDateSort, page1.last, Some(page1.token))
		val page2Results = toItems(page2.results)
		page2Results.map{ _.name } should be ((91 to 95).reverse.map{ i => s"item$i"} )
	}

	/** Check that retrieving paged results according to an overall search works,
		* and doesn't do anything annoying like only sorting within the N results in the page. */
	@Test
	def pagedSearchSortedByDate() {
		indexFakeItems()
		val query = dateRangeQuery

		val pages = for (p <- 0 to 9) yield toItems(service.search(query, 10, service.reverseDateSort, p*10))
		pages.length should be (10)
		pages.foreach { _.length should be (10) }
		pages.flatten should equal (fakeItems.reverse)
	}

	@Test
	def pagedSearchSortedByName() {
		indexFakeItems()
		val query = new WildcardQuery(new Term("name", "*"))

		val sort = new Sort(new SortField("name", SortField.Type.STRING, false))
		val pages = for (p <- 0 to 9) yield toItems(service.search(query, 10, sort, p*10))
		pages.length should be (10)
		pages.foreach { _.length should be (10) }
		pages.flatten.map(_.name) should equal (fakeItems.map(_.name).sorted)
	}

	@Test
	def pagedSearchWithoutSort() {
		indexFakeItems()
		val query = new WildcardQuery(new Term("name", "*"))

		val pages = for (p <- 0 to 9) yield toItems(service.search(query, 10, null, p*10))
		pages.length should be (10)
		pages.foreach { _.length should be (10) }
		// convert to set to ignore order (because results are unsorted)
		pages.flatten.toSet should equal (fakeItems.toSet)
	}

	def toItems(result: RichSearchResults) = result.transformAll(service.convert)

	def indexFakeItems() {
		// Index at random to ensure things don't depend on ordered insertion
		service.indexItems(Random.shuffle(fakeItems))
	}

	/** Simplest implementation of AbstractIndexService to test its core functions.
		* Indexes a simple case class with a name and a date field.
		*/
	class MockIndexService extends AbstractIndexService[Item] with RAMDirectoryOverride {
		val MaxBatchSize: Int = 1000
		val IncrementalBatchSize: Int = 1000

		val analyzer: Analyzer = new StandardAnalyzer
		val UpdatedDateField: String = "date"
		val IdField: String = "name"

		protected def listNewerThan(startDate: DateTime, batchSize: Int): Seq[Item] = ???
		protected def getUpdatedDate(item: Item): DateTime = item.date
		protected def getId(item: Item): String = item.name

		def convert(docs: Seq[Document]) = toItems(docs)

		def clearSearcher(searcherToken: Long) {
			val searcher = this.searcherLifetimeManager.acquire(searcherToken)
			this.searcherLifetimeManager.release(searcher)
			this.searcherLifetimeManager.prune(new Pruner {
				override def doPrune(ageSec: Double, searcher: IndexSearcher): Boolean = true
			})
		}

		protected def toDocuments(item: Item): Seq[Document] = {
			val doc = new Document()
			doc.add( plainStringField(IdField, item.name ) )
			doc.add( dateField(UpdatedDateField, item.date) )
			doc.add( docValuesField(UpdatedDateField, item.date.getMillis) )
			Seq(doc)
		}

		protected def toItems(docs: Seq[Document]): Seq[Item] = docs.map {
			doc => Item(
					name = doc.get(IdField),
					date = new DateTime(doc.get(UpdatedDateField).toLong)
			)
		}

	}


}
