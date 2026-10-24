# Changelog

## Unreleased

### Added

- The world clock is scaled: `doDaylightCycle` is turned off on overworld-type worlds and Timescale advances their time every tick, restoring the gamerule when the plugin is disabled.
- `doDaylightCycle` is turned off on overworld-type worlds loaded after start, so a world loaded or reloaded later is not advanced by both vanilla and Timescale.
- Separate `day-ratio` and `night-ratio` settings in `config.yml` (both default to `1.0`, vanilla speed). See [CONFIG.md](CONFIG.md).
