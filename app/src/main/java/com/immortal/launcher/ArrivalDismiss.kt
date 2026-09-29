/*
 * Copyright (c) 2026 Starbright Lab.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.immortal.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * "Exit when someone walks in" ([ScreensaverConfig.Settings.dismissOnArrival]): a screensaver
 * surface — the dream or the continuation frame — dismisses itself as if tapped when Meta's
 * detector reports an arrival ([isArrival]).
 *
 * Two cases, because the arrival and the screensaver race:
 *  - Someone walks into an empty room and the Portal wakes *into* the dream. The detector's
 *    heartbeat usually lands before the dream attaches, so the surface checks for an arrival in
 *    the last [RECENT_ARRIVAL_MS] as it starts.
 *  - The frame is already up (Always-on, or the room emptied under it) and someone comes back:
 *    a presence listener catches the new arrival.
 */
object ArrivalDismiss {
  private const val TAG = "ImmortalArrival"

  /** How recent an arrival still counts as "the reason this screensaver just started". */
  const val RECENT_ARRIVAL_MS = 60_000L

  /** Pure: did someone arrive within [RECENT_ARRIVAL_MS] of [nowMs]? JVM-unit-tested. */
  fun arrivedRecently(nowMs: Long, lastArrivalAtMs: Long): Boolean =
      lastArrivalAtMs > 0L && nowMs - lastArrivalAtMs in 0..RECENT_ARRIVAL_MS

  /** A running watch; [stop] it when the surface goes away. */
  class Watch internal constructor(private val listener: PresenceHub.Listener) {
    fun stop() {
      PresenceHub.removeListener(listener)
    }
  }

  /**
   * Start watching for an arrival on behalf of a screensaver surface. [onArrival] runs once, on
   * the main thread. Returns null (and does nothing) when the setting is off.
   */
  fun watch(context: Context, onArrival: () -> Unit): Watch? {
    if (!ScreensaverConfig.load(context).dismissOnArrival) return null
    val main = Handler(Looper.getMainLooper())
    val seen = PresenceHub.lastArrivalAtMs
    var fired = false
    fun fire(why: String) {
      main.post {
        if (fired) return@post
        fired = true
        Log.i(TAG, "dismissing screensaver: $why")
        onArrival()
      }
    }
    if (arrivedRecently(System.currentTimeMillis(), seen)) fire("arrived just before it started")
    // Listeners run on whichever thread fed the hub (the log reader, for Meta's detector).
    val listener =
        PresenceHub.Listener { _ -> if (PresenceHub.lastArrivalAtMs != seen) fire("someone arrived") }
    PresenceHub.addListener(listener)
    return Watch(listener)
  }
}
