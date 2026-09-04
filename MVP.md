# Timescale — MVP

## Problem

A vanilla Minecraft day is twenty minutes. On a roleplay server that means a market day, a journey and a siege all happen before dinner, and night arrives constantly. Players asked for a "slow timeline" — a clock that does not sprint.

## Target environment

Spigot, Minecraft 26.1, vanilla clients. Entirely server-side; nothing here is visible to a client as anything other than a slower sun.

## The smallest shippable slice

### v1 does

1. **Scale the clock.** Set `doDaylightCycle` to `false` and advance world time on a scheduled task at a configured ratio. Ratio `1.0` is vanilla; `3.0` gives a one-hour day.
2. **Separate day and night ratios.** The most common actual want is long days and *normal-length* nights, not uniformly slow everything. Two numbers, not one.
3. **Keep sleep working.** With `doDaylightCycle` off, sleeping no longer skips the night — it fails silently, and players read that as a bug. v1 detects the sleep threshold being met and advances the clock to morning itself.
4. **`/timescale`** to read the current ratio, and `/timescale set <day> <night>` for operators.
5. **Persist across restarts**, and re-apply the gamerule on world load so a manual `/gamerule` or a world reload does not quietly restore vanilla speed.

### v1 does not

- **Scale anything but the clock.** Crop growth, furnace speed, mob spawn cycles, potion durations and hunger all stay on real ticks. A longer day therefore means more real crops per day, not fewer. Compensating for that is a real design question and a fast way to make a small plugin large.
- **Support per-world ratios.** One ratio per server.
- **Couple to seasons.** Season-varying day length is [Seasons](https://github.com/Dans-Plugins/Seasons)' business, and the two plugins must not both drive the clock — see the open question there.
- **Offer a client-visible clock, calendar or HUD.**
- **Alter the moon phase cycle.**

## Why these non-goals

"Scale the clock" is a weekend. "Scale time" is a rabbit hole — every rate in the game is tuned against a twenty-minute day, and rebalancing them is a server-tuning project, not a plugin feature. Shipping the clock alone lets operators discover which knock-on effects actually bother them before anyone writes code to fix them.

## Open questions

- What ratio actually feels right? Worth running a poll on the server rather than guessing. Anything past about `4.0` makes a night genuinely tedious for anyone without a bed.
- Should the scheduled task run every tick or on a coarser interval? A coarser interval is cheaper but makes the sun visibly step. Probably every tick, with the increment as a fractional accumulator.
- Does anything else on the server read world time to mean "days elapsed"? If so, slowing the clock silently slows it too.

## Dependencies

None. Must coexist with [Seasons](https://github.com/Dans-Plugins/Seasons) — whichever plugin owns `doDaylightCycle` has to be the only one that touches it.
