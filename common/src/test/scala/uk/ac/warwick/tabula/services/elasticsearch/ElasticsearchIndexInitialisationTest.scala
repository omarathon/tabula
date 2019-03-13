package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticClient
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.ElasticsearchTestBase

class ElasticsearchIndexInitialisationTest extends ElasticsearchTestBase {

	override implicit val patienceConfig: PatienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	private trait ElasticsearchIndexSupport extends ElasticsearchClientComponent {
		override val client: ElasticClient = ElasticsearchIndexInitialisationTest.this.client
	}

	private trait Fixture {
		val index = Index("mock-index")
		val indexType = "wibble"

		val service = new ElasticsearchIndexInitialisation with ElasticsearchIndexName with ElasticsearchIndexType with ElasticsearchIndexSupport with AuditEventElasticsearchConfig {
			override val index: Index = Fixture.this.index
			override val indexType: String = Fixture.this.indexType
		}
	}

	@Test
	def indexCreatedOnPropertiesSet(): Unit = new Fixture {
		index.name should not(beCreated)

		service.ensureIndexExists().futureValue should be (true)
		index.name should beCreated

		// Ensure that future runs of afterPropertiesSet don't affect this
		service.ensureIndexExists().futureValue should be (true)
		index.name should beCreated
	}

}
