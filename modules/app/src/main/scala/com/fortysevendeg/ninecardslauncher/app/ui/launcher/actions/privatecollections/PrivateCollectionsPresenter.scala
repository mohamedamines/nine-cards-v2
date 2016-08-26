package com.fortysevendeg.ninecardslauncher.app.ui.launcher.actions.privatecollections

import com.fortysevendeg.ninecardslauncher.app.commons.Conversions
import com.fortysevendeg.ninecardslauncher.app.ui.commons.Presenter
import com.fortysevendeg.ninecardslauncher.app.ui.commons.TasksOps._
import com.fortysevendeg.ninecardslauncher.commons.services.TaskService._
import com.fortysevendeg.ninecardslauncher.process.commons.models._
import com.fortysevendeg.ninecardslauncher.process.device.GetByName
import com.fortysevendeg.ninecardslauncher.process.moment.MomentConversions
import macroid.{ActivityContextWrapper, Ui}

import scalaz.concurrent.Task

class PrivateCollectionsPresenter(actions: PrivateCollectionsActions)(implicit contextWrapper: ActivityContextWrapper)
  extends Presenter
  with Conversions
  with MomentConversions {

  def initialize(): Unit = {
    loadPrivateCollections()
    actions.initialize().run
  }

  def loadPrivateCollections(): Unit = {
    Task.fork(getPrivateCollections.value).resolveAsyncUi(
      onPreTask = () => actions.showLoading(),
      onResult = (privateCollections: Seq[PrivateCollection]) => {
        if (privateCollections.isEmpty) {
          actions.showEmptyMessage()
        } else {
          actions.addPrivateCollections(privateCollections)
        }
      },
      onException = (ex: Throwable) => actions.showContactUsError())
  }

  def saveCollection(privateCollection: PrivateCollection): Unit = {
    Task.fork(di.collectionProcess.addCollection(toAddCollectionRequest(privateCollection)).value).resolveAsyncUi(
      onResult = (c) => actions.addCollection(c) ~ actions.close(),
      onException = (ex) => actions.showContactUsError())
  }

  private[this] def getPrivateCollections:
  TaskService[Seq[PrivateCollection]] =
    for {
      collections <- di.collectionProcess.getCollections
      moments <- di.momentProcess.getMoments
      apps <- di.deviceProcess.getSavedApps(GetByName)
      unformedApps = toSeqUnformedApp(apps)
      newCollections <- di.collectionProcess.generatePrivateCollections(unformedApps)
      newMomentCollections <- di.momentProcess.generatePrivateMoments(unformedApps map toApp, newCollections.length)
    } yield {
      val privateCollections = newCollections filterNot { newCollection =>
        newCollection.appsCategory match {
          case Some(category) => (collections flatMap (_.appsCategory)) contains category
          case _ => false
        }
      }
      val privateMoments = newMomentCollections filterNot { newMomentCollection =>
        moments find (_.momentType == newMomentCollection.moment) exists (_.collectionId.isDefined)
      }
      privateMoments ++ privateCollections
    }

}

trait PrivateCollectionsActions {

  def initialize(): Ui[Any]

  def showLoading(): Ui[Any]

  def showContactUsError(): Ui[Any]

  def showEmptyMessage(): Ui[Any]

  def addPrivateCollections(privateCollections: Seq[PrivateCollection]): Ui[Any]

  def addCollection(collection: Collection): Ui[Any]

  def close(): Ui[Any]
}