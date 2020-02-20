package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import uk.ac.warwick.tabula.JavaImports._

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class ExamQuestion extends GeneratedId {

  def this(exam: Exam, name: String) = {
    this()
    this.exam = exam
    exam.questions.add(this)
    this.name = name
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id")
  var exam: Exam = _

  @OneToMany(mappedBy = "_parent", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var children: JList[ExamQuestion] = JArrayList()

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent")
  private var _parent: ExamQuestion = _
  def parent: Option[ExamQuestion] = Option(_parent)
  def parent_=(question: ExamQuestion): Unit = _parent = question

  @Column(name = "name")
  private var _name: String = _
  def name_=(name: String): Unit = _name = name
  def name: String = parent match {
    case None => _name
    case Some(p) => s"${p.name}-${_name}"
  }

  @Column(name = "score")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  private var _score: Option[Int] = Option.empty[Int]
  def score_=(max: Int): Unit = _score = Option(max)
  def score: Option[Int] = if(children.isEmpty) _score else {
    val childrenScores = children.asScala.flatMap(_.score)
    if (childrenScores.isEmpty) None else Some(childrenScores.sum)
  }

  override def toString: String = name
}

/**
 * A restriction on the questions that can be answered e.g.
 *
 * One of - 1,2,3
 * Two of 4a 4b 4c 4d 5
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class ExamQuestionRule extends GeneratedId {

  def this(exam: Exam, questions: Seq[ExamQuestion], min:Int, max:Int) = {
    this()
    this.exam = exam
    exam.rules.add(this)
    this.questions.addAll(questions.asJava)
    this.min = min
    this.max = max
  }

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var min: Option[Int] = Option.empty[Int]
  def min_=(m: Int): Unit = min = Option(m)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var max: Option[Int] = Option.empty[Int]
  def max_=(m: Int): Unit = max = Option(m)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id")
  var exam: Exam = _

  @ManyToMany
  @JoinTable(name = "examquestionruleset", joinColumns = Array(new JoinColumn(name = "rule_id")), inverseJoinColumns = Array(new JoinColumn(name = "question_id")))
  @JoinColumn(name = "question_id")
  var questions: JList[ExamQuestion] = JArrayList()

  def meetsRestrictions(answeredQuestions: Set[ExamQuestion]): Boolean = {
    val questionsFromRestriction = answeredQuestions.intersect(questions.asScala.toSet)
    min.forall(m => questionsFromRestriction.size >= m) && max.forall(m => questionsFromRestriction.size <= m)
  }

  def description: String = (min.filterNot(_ == 0), max.filterNot(_ == 0)) match {
    case (Some(min), Some(max)) if min == max => s"$max of ${questions.asScala.mkString(", ")}"
    case (Some(min), Some(max)) => s"At least $min and at most $max of ${questions.asScala.mkString(", ")}"
    case (None, Some(max)) => s"At most $max of ${questions.asScala.mkString(", ")}"
    case (Some(min), None) => s"At least $min of ${questions.asScala.mkString(", ")}"
    case _ => throw new IllegalArgumentException("Invalid AnswerRestriction - either a min or max number of questions must be specified")
  }
}

