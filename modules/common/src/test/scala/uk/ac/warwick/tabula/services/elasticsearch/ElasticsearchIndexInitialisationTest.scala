package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.testkit.{ElasticSugar, IndexMatchers}
import uk.ac.warwick.tabula.TestBase

class ElasticsearchIndexInitialisationTest extends TestBase with ElasticSugar with IndexMatchers {

	private trait ElasticsearchIndexSupport extends ElasticsearchClientComponent {
		override val client = ElasticsearchIndexInitialisationTest.this.client
	}

	private trait Fixture {
		val indexName = "mock"

		val service = new ElasticsearchIndexInitialisation with ElasticsearchIndexName with ElasticsearchIndexSupport with AuditEventElasticsearchConfig {
			override val indexName = Fixture.this.indexName
		}
	}

	@Test
	def indexCreatedOnPropertiesSet(): Unit = new Fixture {
		indexName should not(beCreated)

		service.afterPropertiesSet()
		indexName should beCreated

		// Ensure that future runs of afterPropertiesSet don't affect this
		service.afterPropertiesSet()
		indexName should beCreated
	}

}
