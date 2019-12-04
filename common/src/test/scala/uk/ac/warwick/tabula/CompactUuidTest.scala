package uk.ac.warwick.tabula

import org.junit.Test

class CompactUuidTest extends TestBase {
  @Test def compacting(): Unit = {
    val uuid = "16164939-101f-427b-ae87-d237c127843e"
    val compact = "FhZJORAfQnuuh9I3wSeEPg"

    CompactUuid.compact(uuid) should be(Some(compact))
    CompactUuid.compact("Invalid stuff") should be(None)

    CompactUuid.uncompact(compact) should be(Some(uuid))
    CompactUuid.uncompact(compact + "9") should be(None)
  }

  @Test def compactingHighBytes(): Unit = {
    val uuid = "16164939-101f-427b-ae87-d237c127843e"
    val compact = "ᘖ䤹ဟ䉻꺇툷섧萾"

    CompactUuid.compactHighByte(uuid) should be(compact)
    an[IllegalArgumentException] should be thrownBy { CompactUuid.compactHighByte("Invalid stuff") }

    CompactUuid.uncompactHighByte(CompactUuid.compactHighByte(uuid)) should be(uuid)

    CompactUuid.uncompactHighByte(compact) should be(uuid)
    an[IllegalArgumentException] should be thrownBy { CompactUuid.uncompactHighByte(compact + "9") }
  }
}
