# Configuration

Timescale writes `plugins/Timescale/config.yml` on first start. Changes take effect on the next server start.

| Key | Default | Meaning |
|---|---|---|
| `day-ratio` | `1.0` | How many times longer the day half of the cycle (time of day `0`–`11999`) lasts. `1.0` is vanilla; `3.0` makes the day three times longer. |
| `night-ratio` | `1.0` | How many times longer the night half of the cycle (time of day `12000`–`23999`) lasts. `1.0` is vanilla. |

A vanilla day and night are ten minutes each, so `day-ratio: 3.0` and `night-ratio: 1.0` give a thirty-minute day followed by a ten-minute night. Ratios below `1.0` speed that half up.

A ratio that is missing, not a number, zero, negative or not finite falls back to `1.0`, and a warning naming the key is written to the server log.

## What Timescale changes

- `doDaylightCycle` is set to `false` on every overworld-type world on start, and Timescale advances the time of those worlds itself every tick. It is set back to `true` when the plugin is disabled.
- The nether, the end and any other non-overworld world are left alone.
- Only the clock is scaled. Crop growth, furnaces, mob spawning, potions and hunger stay on real ticks, so a longer day means more of them per day.
