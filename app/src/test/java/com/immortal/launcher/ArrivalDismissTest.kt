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

/**
 * "Exit when someone walks in": what counts as an arrival ([isArrival]) and whether one is recent
 * enough to explain a screensaver that is just starting ([ArrivalDismiss.arrivedRecently]).
 */
class ArrivalDismissTest {

  private fun state(presence: Presence, source: PresenceSource, screen: ScreenState = ScreenState.DREAMING) =
      PresenceState(presence, screen, confident = true, sinceMs = 0L, source = source)

  @Test
  fun detectorSeesSomeoneInAnEmptyRoom_isArrival() {
    assertTrue(
        isArrival(
            state(Presence.ABSENT, PresenceSource.PORTAL, ScreenState.OFF),
            state(Presence.PRESENT, PresenceSource.PORTAL, ScreenState.OFF)))
  }

  @Test
  fun proxyEmptyThenDetectorPresent_isArrival() {
    // The room was read as empty from a sleep at timeout; the detector then speaks up.
    assertTrue(
        isArrival(
            state(Presence.ABSENT, PresenceSource.PROXY, ScreenState.OFF),
            state(Presence.PRESENT, PresenceSource.PORTAL, ScreenState.OFF)))
  }

  @Test
  fun proxyPresence_isNeverAnArrival() {
    // The proxy says PRESENT because the dream started; counting it would dismiss every dream.
    assertFalse(
        isArrival(
            state(Presence.ABSENT, PresenceSource.PROXY, ScreenState.OFF),
            state(Presence.PRESENT, PresenceSource.PROXY)))
  }

  @Test
  fun stillPresent_orFromUnknown_isNotAnArrival() {
    assertFalse(
        isArrival(state(Presence.PRESENT, PresenceSource.PORTAL, ScreenState.INTERACTIVE), state(Presence.PRESENT, PresenceSource.PORTAL)))
    assertFalse(isArrival(state(Presence.UNKNOWN, PresenceSource.PROXY), state(Presence.PRESENT, PresenceSource.PORTAL)))
  }

  @Test
  fun leaving_isNotAnArrival() {
    assertFalse(isArrival(state(Presence.PRESENT, PresenceSource.PORTAL), state(Presence.ABSENT, PresenceSource.PORTAL)))
  }

  @Test
  fun arrivedRecently_withinTheWindowOnly() {
    val now = 1_000_000L
    assertTrue(ArrivalDismiss.arrivedRecently(now, now - 5_000))
    assertTrue(ArrivalDismiss.arrivedRecently(now, now - ArrivalDismiss.RECENT_ARRIVAL_MS))
    assertFalse(ArrivalDismiss.arrivedRecently(now, now - ArrivalDismiss.RECENT_ARRIVAL_MS - 1))
  }

  @Test
  fun arrivedRecently_neverWithoutAnArrival_orFromTheFuture() {
    assertFalse(ArrivalDismiss.arrivedRecently(1_000_000L, 0L))
    assertFalse(ArrivalDismiss.arrivedRecently(1_000_000L, 1_000_500L))
  }
}
