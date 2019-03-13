package uk.ac.warwick.tabula.services.fileserver

import scala.util.matching.Regex

/**
  * Grokked from play.api.mvc.Range and friends
  */
private[fileserver] case class ByteRange(start: Long, end: Long) extends Ordered[ByteRange] {

  override def compare(that: ByteRange): Int = {
    val startCompare = this.start - that.start
    if (startCompare != 0) startCompare.toInt
    else (this.end - that.end).toInt
  }

  def length: Long = this.end - this.start

  def distance(other: ByteRange): Long = {
    mergedEnd(other) - mergedStart(other) - (length + other.length)
  }

  private def mergedStart(other: ByteRange) = math.min(start, other.start)
  private def mergedEnd(other: ByteRange) = math.max(end, other.end)
}

private[fileserver] trait Range extends Ordered[Range] {

  def start: Option[Long]

  def end: Option[Long]

  // For byte ranges, a sender SHOULD indicate the complete length of the
  // representation from which the range has been extracted, unless the
  // complete length is unknown or difficult to determine.  An asterisk
  // character ("*") in place of the complete-length indicates that the
  // representation length was unknown when the header field was generated.
  def getEntityLength: Option[Long]

  def byteRange: ByteRange

  def merge(other: Range): Range

  def length: Long = byteRange.length + 1 // the range end is inclusive

  // RFC 7233:
  // 1. A byte-range-spec is invalid if the last-byte-pos value is present
  //    and less than the first-byte-pos.
  // 2. For byte ranges, failing to overlap the current extent means that the
  //    first-byte-pos of all of the byte-range-spec values were greater than
  //    the current length of the selected representation.
  def isValid: Boolean = getEntityLength match {
    case Some(entityLen) =>
      val br = this.byteRange
      br.start <= br.end && br.start < entityLen
    case None =>
      val br = this.byteRange
      br.start <= br.end
  }

  override def toString: String = {
    val br = this.byteRange
    s"${br.start}-${br.end}"
  }

  override def compare(that: Range): Int = this.byteRange.compare(that.byteRange)
}

private[fileserver] case class WithEntityLengthRange(entityLength: Long, start: Option[Long], end: Option[Long]) extends Range {

  override def getEntityLength = Some(entityLength)

  // Rules according to RFC 7233:
  // 1. If the last-byte-pos value is absent, or if the value is greater
  //    than or equal to the current length of the representation data,
  //    the byte range is interpreted as the remainder of the representation
  //    (i.e., the server replaces the value of last-byte-pos with a value
  //    that is one less than the current length of the selected representation)
  // 2. A client can request the last N bytes of the selected representation
  //    using a suffix-byte-range-spec. If the selected representation is shorter
  //    than the specified suffix-length, the entire representation is used.
  lazy val byteRange: ByteRange = {
    (start, end) match {
      case (Some(_start), Some(_end)) => ByteRange(_start, math.min(_end, entityLength - 1))
      case (Some(_start), None) => ByteRange(_start, entityLength - 1)
      case (None, Some(_end)) => ByteRange(math.max(0, entityLength - _end), entityLength - 1)
      case (None, None) => ByteRange(0, 0)
    }
  }

  def merge(other: Range): Range = {
    val thisByteRange = this.byteRange
    val otherByteRange = other.byteRange
    WithEntityLengthRange(
      entityLength,
      Some(math.min(thisByteRange.start, otherByteRange.start)),
      Some(math.max(thisByteRange.end, otherByteRange.end))
    )
  }
}

private[fileserver] case class WithoutEntityLengthRange(start: Option[Long], end: Option[Long]) extends Range {

  override def getEntityLength: Option[Long] = None

  override def isValid: Boolean = start.nonEmpty && end.nonEmpty && super.isValid

  override def merge(other: Range): Range = {
    val thisByteRange = this.byteRange
    val otherByteRange = other.byteRange
    WithoutEntityLengthRange(
      Some(math.min(thisByteRange.start, otherByteRange.start)),
      Some(math.max(thisByteRange.end, otherByteRange.end))
    )
  }

  override def byteRange: ByteRange = {
    (start, end) match {
      case (Some(_start), Some(_end)) => ByteRange(_start, _end)
      case (_, _) => ByteRange(0, 0)
    }
  }
}

private[fileserver] object Range {

  // Since the typical overhead between parts of a multipart/byteranges
  // payload is around 80 bytes, depending on the selected representation's
  // media type and the chosen boundary parameter length, it can be less
  // efficient to transfer many small disjoint parts than it is to transfer
  // the entire selected representation.
  val minimumDistance = 80 // TODO this should be configurable

  // A server that supports range requests MAY ignore or reject a Range
  // header field that consists of [...] a set of many small ranges that
  // are not listed in ascending order, since both are indications of either
  // a broken client or a deliberate denial-of-service attack (Section 6.1).
  // http://tools.ietf.org/html/rfc7233#section-6.1
  val maxNumberOfRanges = 16 // TODO this should be configurable

  val RangePattern: Regex = """(\d*)-(\d*)""".r

  def apply(entityLength: Option[Long], range: String): Option[Range] = range match {
    case RangePattern(first, last) =>
      val firstByte = asOptionLong(first)
      val lastByte = asOptionLong(last)

      if ((firstByte ++ lastByte).isEmpty) return None // unsatisfiable range

      entityLength
        .map(entityLen => WithEntityLengthRange(entityLen, firstByte, lastByte))
        .orElse(Some(WithoutEntityLengthRange(firstByte, lastByte)))
    case _ => None // unsatisfiable range
  }

  private def asOptionLong(string: String) = if (string.isEmpty) None else Some(string.toLong)
}

private[fileserver] trait RangeSet {

  def ranges: Seq[Option[Range]]

  def entityLength: Option[Long]

  // Rules according to RFC 7233:
  // 1. If a valid byte-range-set includes at least one byte-range-spec with
  //    a first-byte-pos that is less than the current length of the
  //    representation, or at least one suffix-byte-range-spec with a
  //    non-zero suffix-length, then the byte-range-set is satisfiable.
  //    Otherwise, the byte-range-set is unsatisfiable.
  // 2. A server that supports range requests MAY ignore or reject a Range
  //    header field that consists of more than two overlapping ranges, or a
  //    set of many small ranges that are not listed in ascending order,
  //    since both are indications of either a broken client or a deliberate
  //    denial-of-service attack.
  // 3. When multiple ranges are requested, a server MAY coalesce any of the
  //    ranges that overlap, or that are separated by a gap that is smaller
  //    than the overhead of sending multiple parts, regardless of the order
  //    in which the corresponding byte-range-spec appeared in the received
  //    Range header field
  // 4. When a multipart response payload is generated, the server SHOULD
  //    send the parts in the same order that the corresponding
  //    byte-range-spec appeared in the received Range header field,
  //    excluding those ranges that were deemed unsatisfiable or that were
  //    coalesced into other ranges.
  def normalize: RangeSet = {
    if (isValid) {
      flattenRanges.sorted match {
        case seq if seq.isEmpty => UnsatisfiableRangeSet(entityLength)
        case seq => SatisfiableRangeSet(entityLength, ranges = coalesce(seq.toList).map(Option.apply))
      }
    } else {
      UnsatisfiableRangeSet(entityLength)
    }
  }

  private def coalesce(rangeSeq: List[Range]): List[Range] = {
    rangeSeq.foldLeft(List.empty[Range]) { (coalesced, current) =>
      val (mergeCandidates, otherCandidates) = coalesced.partition(_.byteRange.distance(current.byteRange) <= Range.minimumDistance)
      val merged = mergeCandidates.foldLeft(current)(_ merge _)
      otherCandidates :+ merged
    }
  }

  def first: Range = ranges.head match {
    case Some(r) => r
    case None =>
      entityLength
        .map(entityLen => WithEntityLengthRange(entityLength = entityLen, start = Some(0), end = Some(entityLen)))
        .getOrElse(WithoutEntityLengthRange(start = Some(0), end = None))
  }

  private def isValid: Boolean = flattenRanges.forall(_.isValid)

  private def flattenRanges = ranges.filter(_.isDefined).flatten

  override def toString: String = {
    val entityLen = entityLength.map(_.toString).getOrElse("*")
    s"bytes ${flattenRanges.mkString(",")}/$entityLen"
  }
}

private[fileserver] abstract class DefaultRangeSet(entityLength: Option[Long]) extends RangeSet {
  override def ranges: Seq[Option[Range]] = Seq.empty
}

private[fileserver] case class SatisfiableRangeSet(entityLength: Option[Long], override val ranges: Seq[Option[Range]]) extends DefaultRangeSet(entityLength)

private[fileserver] case class UnsatisfiableRangeSet(entityLength: Option[Long]) extends DefaultRangeSet(entityLength) {
  override def toString: String = s"""bytes */${entityLength.getOrElse("*")}"""
}

private[fileserver] case class NoHeaderRangeSet(entityLength: Option[Long]) extends DefaultRangeSet(entityLength)

private[fileserver] object RangeSet {

  // According to RFC 7233:
  //
  //     An origin server MUST ignore a Range header field that contains a
  //     range unit it does not understand.  A proxy MAY discard a Range
  //     header field that contains a range unit it does not understand.
  val WithEntityLengthRangeSetPattern: Regex = """^bytes=[0-9,-]+""".r

  val WithoutEntityLengthRangeSetPattern: Regex = """^bytes=([0-9]+-[0-9]+,?)+""".r

  def apply(entityLength: Option[Long], rangeHeader: Option[String]): RangeSet = rangeHeader match {
    case Some(header) =>
      entityLength.map(_ => {
        header match {
          case WithEntityLengthRangeSetPattern() => headerToRanges(entityLength, header)
          case _ => NoHeaderRangeSet(entityLength)
        }
      }).getOrElse(
        header match {
          case WithoutEntityLengthRangeSetPattern(_) => headerToRanges(entityLength, header)
          case _ => NoHeaderRangeSet(entityLength)
        }
      ).normalize
    case None => NoHeaderRangeSet(entityLength)
  }

  private def headerToRanges(entityLength: Option[Long], header: String): RangeSet = {
    val ranges = header.split("=")(1).split(",").map { r => Range(entityLength, r) }
    SatisfiableRangeSet(entityLength, ranges)
  }
}
