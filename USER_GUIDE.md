# User Guide

This guide is for server operators. For the exact keys and commands, see [CONFIG.md](CONFIG.md) and [COMMANDS.md](COMMANDS.md).

## What Timescale does

Timescale makes the in-game clock run slower or faster than vanilla, with one ratio for the day and another for the night. It turns off the daylight-cycle gamerule on overworld-type worlds and advances their time itself on every server tick.

Only the clock changes. Crops, furnaces, mob spawning, potions and hunger stay on real ticks, so a longer day means more of them per day. The nether and the end are left alone.

## Installing

Timescale has not had a release yet. A rolling development build of `main` is published as the [`dev` prerelease](https://github.com/Dans-Plugins/Timescale/releases/tag/dev). It is unreleased code, so back up your server before trying it.

1. Put the jar in the server's `plugins/` directory.
2. Restart the server. Timescale writes `plugins/Timescale/config.yml`.
3. Run `/timescale`. Both ratios start at `1.0`, vanilla speed.

The versions Timescale is built to support are listed in [`minecraft-versions.json`](minecraft-versions.json). CI confirms that the plugin's Bukkit API use exists on each of them, but running on a real 1.21.11 or 26.x server is still to be confirmed ([#11](https://github.com/Dans-Plugins/Timescale/issues/11)).

## Choosing ratios

A ratio says how many times longer that half of the cycle lasts. A vanilla day and a vanilla night are ten minutes each, so each half lasts ten real minutes multiplied by its ratio:

| `day-ratio` | `night-ratio` | Day | Night | Full cycle |
|---|---|---|---|---|
| `1.0` | `1.0` | 10 minutes | 10 minutes | 20 minutes (vanilla) |
| `3.0` | `1.0` | 30 minutes | 10 minutes | 40 minutes |
| `3.0` | `3.0` | 30 minutes | 30 minutes | 1 hour |
| `6.0` | `1.0` | 1 hour | 10 minutes | 1 hour 10 minutes |

Long days with normal-length nights are often what people want: a long night is tedious for anyone without a bed. A ratio below `1.0` speeds that half up.

The ratios apply to every overworld-type world on the server. There are no per-world ratios.

## Changing ratios

Run `/timescale set <day> <night>`, for example `/timescale set 3 1`. This needs the `timescale.view` and `timescale.set` permissions; everyone has `timescale.view` and operators have `timescale.set` by default. The new ratios apply from the next tick and are saved to `config.yml`.

You can also edit `day-ratio` and `night-ratio` in `plugins/Timescale/config.yml`. An edit to the file takes effect on the next server start. A value that is not a positive number falls back to `1.0`, with a warning in the server log.

## Sleeping

Sleeping still skips the night. Timescale follows the vanilla sleeping-percentage gamerule, which defaults to every player. Once enough players have been in bed for 100 ticks (five seconds), the world moves to the next morning. Each world is checked on its own. See [CONFIG.md](CONFIG.md#what-timescale-changes) for how players are counted.

## Living with a scaled clock

- `/time set` works as usual, and the clock carries on from the new time at the same ratios.
- A manual `/gamerule doDaylightCycle true` (`advance_time` on 1.21.11 and later) on a world Timescale drives is undone on the next tick. To hand the clock back to vanilla, disable or remove Timescale.
- Another plugin that reads world time to mean "days elapsed" sees days pass more slowly.
- Do not run Timescale alongside another plugin that drives the clock or the daylight-cycle gamerule, including [Seasons](https://github.com/Dans-Plugins/Seasons). Timescale turns the gamerule off again on every tick, so the two would fight over it.

## Removing Timescale

1. Stop the server.
2. Remove the jar from `plugins/`.
3. Start the server.

When Timescale is disabled it turns the daylight-cycle gamerule back on for every loaded overworld-type world. A world that was not loaded at that moment keeps the rule off, and so does every world if the server crashed instead of stopping cleanly. If a world's clock is frozen after removing Timescale, run `/gamerule doDaylightCycle true` in it (`/gamerule advance_time true` on 1.21.11 and later).

## Troubleshooting

| What you see | What to check |
|---|---|
| Time runs at vanilla speed | Run `/timescale`. Both ratios default to `1.0`. An edit to `config.yml` needs a server restart. |
| `Could not find the daylight-cycle gamerule` in the log, and the plugin disables itself | The server has neither gamerule name Timescale knows. Vanilla keeps the clock. Please [report it](https://github.com/Dans-Plugins/Timescale/issues/new) with your server version. |
| `Could not find the sleeping-percentage gamerule` in the log | Timescale still runs, but every player must sleep to skip the night. |
| `day-ratio must be ...` or `night-ratio must be ...` in the log | That value in `config.yml` was rejected, and `1.0` is in use until it is corrected. |
| Sleeping does not skip the night | Check how many players are needed with the sleeping-percentage gamerule. Spectators are not counted, and at least one player must have been in bed for 100 ticks. |
| `/timescale` or `/timescale set` is refused by the server | The whole command needs `timescale.view`, and `set` also needs `timescale.set`. See [COMMANDS.md](COMMANDS.md#permissions). |
