package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.CatsPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import io.circe.parser._
import org.ergoplatform.dex.tracker.streaming.{KafkaMempoolEvent, MempoolEvent}

class KafkaEventsParser extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  val mempoolString = """{"TxAccepted":{"tx":"Aq5Tjhs7D7DHKeeqUM12h8DdR21ZH0bxw0bANVo3BOtHAABD6tGobdar1ZpW4oXL+wfjH/RWqed1d2qAnOmxLmWt6DiZ4YxAxP64KVhuEQ0KbuJjiK0voKn5IN9bQx5UxeDiNCN6JmWpTICsTIgolnItJYa37X5mRRWk5AAAA5kW11EyWTyLB/4YvY1YO9oWUu7XVlz0Gkc43dkPyZLsMD85AmVyvLQGC1H6/JN4eiNrskN0S6uqmfzrgz1h4ZgD+vLLMp8ukNbSO1jZG7tsBGqhQyYcwh9S++KCS/y/BAPs4a2GnasoGZkDDwQABAIEAgQEBAQF/v//////////AQX+//////////8BBQAE0A8EAAQABAYFAAUABYDaxAnYGdYBsqVzAADWAuTGpwQE1gPbYwhyAdYE22MIp9YFsnIDcwEA1gaycgRzAgDWB7JyA3MDANYIsnIEcwQA1gmZcwWMcgYC1gqZmXMGjHIFAnIJ1gvBcgHWDMGn1g2ZcgtyDNYOkXINcwfWD4xyCALWEH5yDwbWEX5yDQbWEpmMcgcCcg/WE35yDAbWFHMI1hV+chIG1hZ+cgoG1hd+cgkG1hicchFyF9YZnHIVchfR7e3t7e3t7ZPCcgHCp5PkxnIBBARyApOycgNzCQCycgRzCgCTjHIFAYxyBgGTjHIHAYxyCAGTsXIDcwuVk3IKcwyVcg6SnJxyEHIRfnICBpx+8HISBpqcchN+chQGfpxyDX5yAgUGkpycchNyFX5yAgacfvByDQaanHIQfnIUBn6cchJ+cgIFBpXtcg6RchJzDZByFqGdchhyE51yGXIQ7ZJyGJxyFnITknIZnHIWchCRcgtzDtPmRQMAAQHPnqPBwv7//38CsfeSCgEExg/zqdeZ4S8ACM0CPF2XoH539G0EhJTqcSl5LLruN2mWL9df0b1Wyk+FL5rT5kUBAv+LAQEEqM2LAYCt4gQQBQQABAAONhACBKALCM0Ceb5mfvncu6xVoGKVzocLBwKb/NstzijZWfKBWxb4F5jqAtGSo5qMx6cBcwBzARABAgQC0ZaDAwGTo4zHsqVzAAABk8KypXMBAHRzAnMDgwEIze6sk7GlcwTT5kUAAA"}}"""

  property("Parse mempool tx correct") {
    val a = parse(mempoolString).toOption.get
      .as[KafkaMempoolEvent]
      .toOption.get

    val b = MempoolEvent.fromKafkaEvent(a)

    println(b)
  }
}
