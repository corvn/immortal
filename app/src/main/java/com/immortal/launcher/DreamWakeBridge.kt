/*
 * Copyright (c) 2026 Starbright Lab.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.immortal.launcher

import android.service.dreams.DreamService
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Wakes Immortal's active DreamService when Home Assistant opens Assist from a wake word.
 *
 * Portal firmware leaves an interactive dream above activities launched by a voice-interaction
 * service. Home Assistant detects the wake word and starts its Assist activity, but the user keeps
 * seeing the photo frame and the listening UI is hidden underneath it. [BarWatchService] observes
 * that activity transition and routes it here.
 */
internal object DreamWakeBridge {
  private const val TAG = "ImmortalDream"
  private const val HA_PACKAGE = "io.homeassistant.companion.android"
  private const val HA_MINIMAL_PACKAGE = "$HA_PACKAGE.minimal"
  private const val HA_ASSIST_ACTIVITY = "$HA_PACKAGE.assist.AssistActivity"

  @Volatile private var activeDream: WeakReference<DreamService>? = null
  @Volatile private var wakeRequested = false

  @Synchronized
  fun attach(dream: DreamService) {
    activeDream = WeakReference(dream)
    wakeRequested = false
  }

  @Synchronized
  fun detach(dream: DreamService) {
    if (activeDream?.get() === dream) {
      activeDream = null
      wakeRequested = false
    }
  }

  /** Return true only for the official HA companion app's Assist activity. */
  fun isHomeAssistantAssistActivity(packageName: CharSequence?, className: CharSequence?): Boolean =
      (packageName == HA_PACKAGE || packageName == HA_MINIMAL_PACKAGE) &&
          className == HA_ASSIST_ACTIVITY

  /** Wake the current dream once; repeated accessibility events are harmless. */
  @Synchronized
  fun wakeForVoiceAssistant(): Boolean {
    val dream = activeDream?.get() ?: return false
    if (wakeRequested) return false
    wakeRequested = true

    // Treat voice activation like a user tap. Without this marker, ACTION_DREAMING_STOPPED can be
    // classified as a system bounce and DreamPolicy immediately puts the photo frame back on top.
    DreamPolicy.userExitAt = System.currentTimeMillis()
    return runCatching {
          Log.i(TAG, "Home Assistant Assist opened; waking active dream")
          dream.wakeUp()
          true
        }
        .onFailure {
          wakeRequested = false
          Log.w(TAG, "couldn't wake dream for Home Assistant Assist", it)
        }
        .getOrDefault(false)
  }
}
