# Changelog

## Unreleased

### Added

- The world clock is scaled: `doDaylightCycle` is turned off on overworld-type worlds and Timescale advances their time every tick, restoring the gamerule when the plugin is disabled.
- `doDaylightCycle` is turned off on overworld-type worlds loaded after start, so a world loaded or reloaded later is not advanced by both vanilla and Timescale.
- A manual `/gamerule doDaylightCycle true` (`advance_time` on 1.21.11 and later) on an overworld-type world is undone on the next tick, so the world is not advanced by both vanilla and Timescale until the next restart.
- Sleeping skips the night again: when the `playersSleepingPercentage` threshold (`players_sleeping_percentage` on 1.21.11 and later) is met, Timescale moves the world to the next morning, which vanilla stops doing once `doDaylightCycle` is off.
- On a server where neither `advance_time` nor `doDaylightCycle` is found, Timescale logs an error and disables itself, leaving the clock to vanilla. If neither `players_sleeping_percentage` nor `playersSleepingPercentage` is found, it logs a warning and every player must sleep to skip the night.
- Separate `day-ratio` and `night-ratio` settings in `config.yml` (both default to `1.0`, vanilla speed). A ratio that is missing, is not a number or is not finite falls back to `1.0`, and a warning naming the key is written to the server log. See [CONFIG.md](CONFIG.md).
- `/timescale` shows the day and night ratios, and `/timescale set <day> <night>` (permission `timescale.set`, operators by default) changes them without a restart and saves them to `config.yml`. See [COMMANDS.md](COMMANDS.md).
- Ratios have a minimum of `1/24000` (about `0.0000417`). A smaller ratio is refused by `/timescale set` and falls back to `1.0` from `config.yml`, because a small enough ratio would overflow the world's full time and lose the elapsed-day count.
