package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.common.settings.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class ElasticsearchClientFactoryBean extends ScalaFactoryBean[ElasticClient] {

	@Value("${elasticsearch.cluster.name}") var clusterName: String = _
	@Value("${elasticsearch.cluster.nodes}") var nodes: Array[String] = _
	@Value("${elasticsearch.cluster.detect_other_nodes}") var detectOtherNodes: Boolean = _

	override def createInstance(): ElasticClient = {
		val settings =
			Settings.settingsBuilder()
			  .put("cluster.name", clusterName)
				.put("client.transport.sniff", detectOtherNodes)
				.build()

		ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://${nodes.mkString(",")}"))
	}

	override def destroyInstance(instance: ElasticClient): Unit = instance.close()
}

trait ElasticsearchClientComponent {
	def client: ElasticClient
}

trait AutowiringElasticsearchClientComponent extends ElasticsearchClientComponent {
	var client = Wire[ElasticClient]
}
