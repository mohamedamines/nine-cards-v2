package com.fortysevendeg.ninecardslauncher.process.user

import java.io.File

import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.DisplayMetrics
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.process.user.models.Device
import com.fortysevendeg.ninecardslauncher.services.api.models.{Installation, User, GoogleDevice}
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.specification.Scope

trait MockUserProcess
  extends Scope
  with Mockito {

  val userStatusCode = 101

  val userId = "fake-user-id"

  val userToken = "fake-user-token"

  val email = "example@47deg.com"

  val device = Device(
    "Nexus X",
    "",
    "",
    Seq.empty)

  val googleDevice = GoogleDevice(
    device.name,
    device.deviceId,
    device.secretToken,
    device.permissions)

  val user = User(
    Option(userId),
    Option(userToken),
    Option(email),
    Seq(googleDevice))

  val installationStatusCode = 102

  val installationId = "fake-installation-id"
  val installationToken = "fake-user-token"
  val deviceType = Option("ANDROID")

  val initialInstallation = Installation(None, deviceType, None, None)

  val installation = Installation(
    Option(installationId),
    deviceType,
    Option(installationToken),
    Option(userId)
  )

  val fileFolder = "/file/example"

  val appIconDir = mock[File]
  appIconDir.getPath returns fileFolder

  val resources = mock[Resources]
  resources.getDisplayMetrics returns mock[DisplayMetrics]

  val contextSupport = mock[ContextSupport]
  contextSupport.getPackageManager returns mock[PackageManager]
  contextSupport.getAppIconsDir returns appIconDir
  contextSupport.getResources returns resources

}
