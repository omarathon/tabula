
import org.springframework.context._
import org.springframework.context.support._
import uk.ac.warwick.tabula.system._
import org.hibernate._

println("Starting Spring")

object App {
  lazy val context:ApplicationContext = {
    val ctx = new GenericXmlApplicationContext()
    ctx.getEnvironment().setActiveProfiles("console")
    ctx.load("applicationContext.xml")
    ctx.refresh()
    ctx
  }

  def bean[T](name:String) : T = context.getBean(name).asInstanceOf[T]

  def inSession(f: (Session)=>Unit) = {
    val session = bean[SessionFactory]("sessionFactory").openSession
    try {
      f(session)
    } finally {
      if (session != null) session.close()
    } 
  }

  
}

App.context
