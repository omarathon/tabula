package uk.ac.warwick.tabula.services.elasticsearch

import java.io.File

import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class ElasticsearchClientFactoryBean extends ScalaFactoryBean[ElasticClient] {

	@Value("${elasticsearch.cluster.name}") var clusterName: String = _
	@Value("${elasticsearch.cluster.nodes}") var nodes: Array[String] = _

	@Value("${elasticsearch.cluster.local_jvm:false}") var localJvmCluster: Boolean = _
	@Value("${filesystem.index.dir}") var localIndexDirectory: File = _

	override def createInstance(): ElasticClient = {
		if (localJvmCluster) {
			throw new IllegalStateException("Sorry, local JVM cluster ElasticSearch is no longer supported.")
		} else {
			ElasticClient(ElasticProperties(nodes.map { n => s"http://$n" }.mkString(",")))
		}
	}

	override def destroyInstance(instance: ElasticClient): Unit = instance.close()
}

trait ElasticsearchClientComponent {
	def client: ElasticClient
}

trait AutowiringElasticsearchClientComponent extends ElasticsearchClientComponent {
	var client: ElasticClient = Wire[ElasticClient]
}
