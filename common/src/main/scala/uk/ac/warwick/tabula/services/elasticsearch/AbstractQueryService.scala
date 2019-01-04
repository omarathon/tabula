package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.SearchRequest
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractQueryService
	extends ElasticsearchClientComponent
		with ElasticsearchIndexName
		with ElasticsearchIndexType
		with ElasticsearchSearching {

	@Autowired var client: ElasticClient = _

}

trait ElasticsearchSearching {
	self: ElasticsearchClientComponent
		with ElasticsearchIndexName
		with ElasticsearchIndexType =>

	protected def searchRequest: SearchRequest = search(index)

}
