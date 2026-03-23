package org.calypsonet.keyple.demo.control.domain.model

import org.calypsonet.keyple.demo.control.data.model.Location

object AppSettings {
  lateinit var readerType: ReaderType
  lateinit var location: Location
  var validationPeriod: Int = 0
}