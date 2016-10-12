package cards.nine.commons.contentresolver

object NotificationUri {

  val authorityPart = "com.fortysevendeg.ninecardslauncher"

  val contentPrefix = "notification://"

  val baseUriNotificationString = s"$contentPrefix$authorityPart"

  val appUriPath = "app"

  val cardUriPath = "card"

  val collectionUriPath = "collection"

  val dockAppUriPath = "dockApp"

  val momentUriPath = "moment"

  val userUriPath = "user"

  val widgetUriPath = "widget"

}
