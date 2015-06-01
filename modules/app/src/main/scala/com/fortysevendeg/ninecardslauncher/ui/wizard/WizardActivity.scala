package com.fortysevendeg.ninecardslauncher.ui.wizard

import android.accounts.{Account, AccountManager}
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.ActionBarActivity
import android.widget.RadioButton
import com.fortysevendeg.macroid.extras.UIActionsExtras._
import com.fortysevendeg.ninecardslauncher.api.services.{ApiUserConfigService, ApiGooglePlayService, ApiUserService}
import com.fortysevendeg.ninecardslauncher.modules.ComponentRegistryImpl
import com.fortysevendeg.ninecardslauncher.modules.googleconnector.{GoogleOperationCanceledException, RequestTokenRequest}
import com.fortysevendeg.ninecardslauncher.services.CreateCollectionService
import com.fortysevendeg.ninecardslauncher.services.api.GetUserConfigRequest
import com.fortysevendeg.ninecardslauncher.services.api.impl.{ApiServicesImpl, ApiServicesConfig}
import com.fortysevendeg.ninecardslauncher.ui.commons.GoogleServicesConstants._
import com.fortysevendeg.ninecardslauncher2.R
import com.fortysevendeg.rest.client.ServiceClient
import com.fortysevendeg.rest.client.http.OkHttpClient
import macroid.FullDsl._
import macroid.{ContextWrapper, Contexts, Transformer, Ui}
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.macroid.extras.ResourcesExtras._

import scala.concurrent.ExecutionContext.Implicits.global

class WizardActivity
  extends ActionBarActivity
  with Contexts[FragmentActivity]
  with Layout
  with ComponentRegistryImpl {

  self =>

  private var finished = false

  override lazy val contextProvider: ContextWrapper = activityContextWrapper

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

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(layout)
    loadUsers()
  }

  override def onBackPressed(): Unit = {
    if (finished) {
      setResult(Activity.RESULT_OK)
      finish()
      super.onBackPressed()
    }
  }

  def loadUsers(): Unit = {
    val accountManager: AccountManager = AccountManager.get(this)
    val accounts: Seq[Account] = accountManager.getAccountsByType(AccountType).toSeq
    runUi(
      addUsersToRadioGroup(accounts) ~
        (userAction <~ On.click {
          usersGroup <~ selectUser
        }) ~
        (deviceAction <~ On.click {
          devicesGroup <~ selectDevice
        }) ~
        (finishAction <~ On.click {
          Ui {
            setResult(Activity.RESULT_OK)
            finish()
          }
        })
    )
  }

  private def selectUser = Transformer {
    case i: RadioButton if i.isChecked =>
      googleConnectorServices.requestToken(activityContextWrapper)(RequestTokenRequest(i.getTag.toString)) map {
        response => runUi(showLoading ~ Ui(searchDevices()))
      } recover {
        case ex: GoogleOperationCanceledException => runUi(uiShortToast(R.string.canceledGooglePermission) ~ showUser)
        case _ => runUi(uiShortToast(R.string.errorConnectingGoogle) ~ showUser)
      }
      showLoading
  }

  private def selectDevice = Transformer {
    case i: RadioButton if i.isChecked =>
      val tag = i.getTag.toString
      (if (tag == NewConfigurationKey) {
        launchService()
      } else {
        launchService(Option(tag))
      }) ~ showWizard
  }

  private def searchDevices() = {
    val errorUi = uiShortToast(R.string.deviceNotFoundMessage) ~ showUser
    (for {
      user <- userServices.getUser
      token <- user.sessionToken
      androidId <- userServices.getAndroidId
    } yield {
      apiServices.getUserConfig(GetUserConfigRequest(androidId, token)) map {
        response =>
          response.userConfig map {
            userConfig =>
              runUi(addDevicesToRadioGroup(userConfig.devices) ~
                showDevices ~
                (titleDevice <~ tvText(resGetString(R.string. addDeviceTitle, userConfig.plusProfile.displayName))))
          } getOrElse runUi(errorUi)
      } recover {
        case _ => runUi(errorUi)
      }
    }) getOrElse runUi(errorUi)
  }

  private def showLoading =
    (loadingRootLayout <~ vVisible) ~
      (userRootLayout <~ vGone) ~
      (wizardRootLayout <~ vGone) ~
      (deviceRootLayout <~ vGone)

  private def showUser =
    (loadingRootLayout <~ vGone) ~
      (userRootLayout <~ vVisible) ~
      (wizardRootLayout <~ vGone) ~
      (deviceRootLayout <~ vGone)

  private def showWizard =
    (loadingRootLayout <~ vGone) ~
      (userRootLayout <~ vGone) ~
      (wizardRootLayout <~ vVisible) ~
      (deviceRootLayout <~ vGone) ~
      Ui(finished = true)

  private def showDevices =
    (loadingRootLayout <~ vGone) ~
      (userRootLayout <~ vGone) ~
      (wizardRootLayout <~ vGone) ~
      (deviceRootLayout <~ vVisible)

  private def launchService(maybeKey: Option[String] = None) = Ui {
    val intent = new Intent(self, classOf[CreateCollectionService])
    maybeKey map (key => intent.putExtra(CreateCollectionService.KeyDevice, key))
    self.startService(intent)
  }

}
