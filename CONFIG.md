# Configuration

Timescale writes `plugins/Timescale/config.yml` on first start. Changes made by editing the file take effect on the next server start. [`/timescale set <day> <night>`](COMMANDS.md) changes both ratios immediately and saves them to this file.

| Key | Default | Meaning |
|---|---|---|
| `day-ratio` | `1.0` | How many times longer the day half of the cycle (time of day `0`–`11999`) lasts. `1.0` is vanilla; `3.0` makes the day three times longer. |
| `night-ratio` | `1.0` | How many times longer the night half of the cycle (time of day `12000`–`23999`) lasts. `1.0` is vanilla. |

A vanilla day and night are ten minutes each, so `day-ratio: 3.0` and `night-ratio: 1.0` give a thirty-minute day followed by a ten-minute night. Ratios below `1.0` speed that half up.

A ratio that is not a number, zero, negative or not finite falls back to `1.0`, and a warning naming the key is written to the server log. A key removed from the file uses its default of `1.0`.

## What Timescale changes

- `doDaylightCycle` is set to `false` on every overworld-type world on start, and Timescale advances the time of those worlds itself every tick. It is set to `false` again whenever an overworld-type world is loaded after start (for example by a world-management plugin), so a reloaded world does not run at vanilla speed on top of Timescale. It is set back to `true` when the plugin is disabled, so removing Timescale does not leave the clock frozen. On Minecraft 1.21.11 and later the same gamerule is called `advance_time`; Timescale uses whichever name the server has, and disables itself with an error in the log if it finds neither.
- Sleeping still skips the night. With `doDaylightCycle` off, vanilla wakes sleeping players but no longer moves the clock, so Timescale moves an overworld-type world to the next morning itself once enough of its players are asleep. "Enough" follows vanilla: the `playersSleepingPercentage` gamerule (default `100`) of the players who are not spectators, at least one, and each must have been in bed for 100 ticks; players marked as sleeping-ignored count as asleep, but at least one real sleeper is needed. On a server where Timescale cannot find that gamerule, every player must sleep, and a warning is written to the log. Clearing rain and thunder on a successful sleep is left to vanilla, which still does it when `doWeatherCycle` is on.
- The nether, the end and any other non-overworld world are left alone.
- Only the clock is scaled. Crop growth, furnaces, mob spawning, potions and hunger stay on real ticks, so a longer day means more of them per day.
