/*
 * Copyright (c) 2026 Starbright Lab.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.immortal.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamWakeBridgeTest {

  @Test
  fun recognizesAssistActivityFromBothHomeAssistantFlavors() {
    val activity = "io.homeassistant.companion.android.assist.AssistActivity"

    assertTrue(
        DreamWakeBridge.isHomeAssistantAssistActivity(
            "io.homeassistant.companion.android", activity))
    assertTrue(
        DreamWakeBridge.isHomeAssistantAssistActivity(
            "io.homeassistant.companion.android.minimal", activity))
  }

  @Test
  fun rejectsOtherHomeAssistantActivitiesAndPackages() {
    assertFalse(
        DreamWakeBridge.isHomeAssistantAssistActivity(
            "io.homeassistant.companion.android.minimal",
            "io.homeassistant.companion.android.webview.WebViewActivity"))
    assertFalse(
        DreamWakeBridge.isHomeAssistantAssistActivity(
            "example.homeassistant",
            "io.homeassistant.companion.android.assist.AssistActivity"))
    assertFalse(DreamWakeBridge.isHomeAssistantAssistActivity(null, null))
  }
}
