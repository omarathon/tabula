package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaWebhook}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.services.turnitintca.TcaEventType._
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AutowiringTopLevelUrlComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class TurnitinTcaRegisterWebhooksJob extends AutowiredJobBean with AutowiringTurnitinTcaServiceComponent with AutowiringTopLevelUrlComponent with Logging {

  override def executeInternal(context: JobExecutionContext): Unit = if(features.turnitinTca) {

    val checkAndRegisterWebhooks = turnitinTcaService.listWebhooks.flatMap(webhooks => {

      lazy val submissionCompleteWebhook = if (!webhooks.exists(_.description == TcaWebhook.SubmissionWebhook)){
        turnitinTcaService.registerWebhook(
          TcaWebhook(toplevelUrl + Routes.turnitin.webhooks.submissionComplete, TcaWebhook.SubmissionWebhook, Seq(SubmissionComplete))
        )
      } else {
        Future.unit
      }

      lazy val similarityCompleteWebhook = if (!webhooks.exists(_.description == TcaWebhook.SimilarityWebhook)){
        turnitinTcaService.registerWebhook(
          TcaWebhook(toplevelUrl + Routes.turnitin.webhooks.similarityComplete, TcaWebhook.SimilarityWebhook, Seq(SimilarityComplete, SimilarityUpdated))
        )
      } else {
        Future.unit
      }

      submissionCompleteWebhook.flatMap(_ => similarityCompleteWebhook)
    })

    Await.result(checkAndRegisterWebhooks, Duration.Inf)
  }
}
