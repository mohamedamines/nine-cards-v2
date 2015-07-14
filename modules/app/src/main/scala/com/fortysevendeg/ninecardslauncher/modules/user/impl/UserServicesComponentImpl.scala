package com.fortysevendeg.ninecardslauncher.modules.user.impl

import java.io.File

import android.net.Uri
import com.fortysevendeg.ninecardslauncher.api.services.{ApiGooglePlayService, ApiUserConfigService, ApiUserService}
import com.fortysevendeg.ninecardslauncher.commons.ContextWrapperProvider
import com.fortysevendeg.ninecardslauncher.modules.user._
import com.fortysevendeg.ninecardslauncher.services.api.impl.{ApiServicesConfig, ApiServicesImpl}
import com.fortysevendeg.ninecardslauncher.services.api.models.{Installation, User}
import com.fortysevendeg.ninecardslauncher.services.utils.FileUtils
import com.fortysevendeg.ninecardslauncher.ui.commons.GoogleServicesConstants._
import com.fortysevendeg.ninecardslauncher2.R
import com.fortysevendeg.rest.client.ServiceClient
import com.fortysevendeg.rest.client.http.OkHttpClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.\/-

trait UserServicesComponentImpl
  extends UserServicesComponent {

  self: ContextWrapperProvider =>

  lazy val userServices = new UserServicesImpl

  class UserServicesImpl
    extends UserServices
    with FileUtils {

    val DeviceType = "ANDROID"
    val FilenameUser = "__user_entity__"
    val FilenameInstallation = "__installation_entity__"

    private val BasicInstallation = Installation(id = None, deviceType = Some(DeviceType), deviceToken = None, userId = None)

    private var synchronizingChangesInstallation: Boolean = false
    private var pendingSynchronizedInstallation: Boolean = false

    private lazy val serviceClient = new ServiceClient(
      httpClient = new OkHttpClient(),
      baseUrl = contextProvider.application.getString(R.string.api_base_url))

    private lazy val apiServices = new ApiServicesImpl(
      ApiServicesConfig(
        contextProvider.application.getString(R.string.api_app_id),
        contextProvider.application.getString(R.string.api_app_key),
        contextProvider.application.getString(R.string.api_localization)),
      new ApiUserService(serviceClient),
      new ApiGooglePlayService(serviceClient),
      new ApiUserConfigService(serviceClient))

    override def register(): Unit =
      if (!getFileInstallation.exists()) {
        saveInstallation(BasicInstallation)
      }


    override def unregister(): Unit = {
      saveInstallation(BasicInstallation)
      synchronizeInstallation()
      val fileUser = getFileUser
      if (fileUser.exists()) fileUser.delete()
    }

    // TODO We have to store the information in Database. Serialization it's temporarily
    override def getUser: Option[User] =
      loadFile[User](getFileUser) match {
        case Success(us) => Some(us)
        case Failure(ex) => None
      }

    override def getInstallation: Option[Installation] =
      loadFile[Installation](getFileInstallation) match {
        case Success(inst) => Some(inst)
        case Failure(ex) => None
      }

    override def signIn: LoginRequest => Future[SignInResponse] =
      request => {
        apiServices.login(request) map {
          response =>
            response.user map {
              user =>
                saveUser(user)
                getInstallation map {
                  i =>
                    saveInstallation(i.copy(userId = user.id))
                    synchronizeInstallation()
                }
                SignInResponse(response.statusCode)
            } getOrElse(throw UserNotFoundException())
        } recover {
          case _ => throw UserUnexpectedException()
        }
      }

    override def getAndroidId: Option[String] = Try {
      val cursor = Option(contextProvider.application.getContentResolver.query(Uri.parse(ContentGServices), null, null, Array(AndroidId), null))
      cursor filter (c => c.moveToFirst && c.getColumnCount >= 2) map (_.getLong(1).toHexString.toUpperCase)
    } match {
      case Success(id) => id
      case Failure(ex) => None
    }

    private def saveInstallation(installation: Installation) = writeFile[Installation](getFileInstallation, installation)

    private def saveUser(user: User) = writeFile[User](getFileUser, user)

    private def getFileInstallation = new File(contextProvider.application.getFilesDir, FilenameInstallation)

    private def getFileUser = new File(contextProvider.application.getFilesDir, FilenameUser)

    private def synchronizeInstallation(): Unit =
      synchronizingChangesInstallation match {
        case true => pendingSynchronizedInstallation = true
        case _ =>
          synchronizingChangesInstallation = true
          getInstallation map {
            inst =>
              inst.id map {
                id =>
                  apiServices.updateInstallation(inst.id, inst.deviceType, inst.deviceToken, inst.userId) map {
                    response =>
                      synchronizingChangesInstallation = false
                      if (pendingSynchronizedInstallation) {
                        pendingSynchronizedInstallation = false
                        synchronizeInstallation()
                      }
                  }
              } getOrElse {
                apiServices.createInstallation(inst.id, inst.deviceType, inst.deviceToken, inst.userId) map {
                  response =>
                    synchronizingChangesInstallation = false
                    response match {
                      case \/-(r) => r.installation map saveInstallation
                      case _ =>
                    }
                    if (pendingSynchronizedInstallation) {
                      pendingSynchronizedInstallation = false
                      synchronizeInstallation()
                    }
                }
              }
          }
      }

  }

}

