package com.fortysevendeg.ninecardslauncher.services.calls

import com.fortysevendeg.ninecardslauncher.commons.services.Service._
import com.fortysevendeg.ninecardslauncher.services.calls.models.Call

trait CallsServices {
  /**
   * Get the last calls in the system
   * @return the Seq[com.fortysevendeg.ninecardslauncher.services.calls.models.Call] contains
   *         information about the widget
   * @throws CallsServicesException if exist some problem to get the calls in the cell phone
   */
  def getLastCalls: ServiceDef2[Seq[Call], CallsServicesException]
}