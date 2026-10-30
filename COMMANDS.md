# Commands

| Command | Permission | Default | What it does |
|---|---|---|---|
| `/timescale` | `timescale.view` | everyone | Shows the current day and night ratios. |
| `/timescale set <day> <night>` | `timescale.set` | operators | Changes both ratios and saves them to `config.yml`. |

## `/timescale`

Replies with the ratios the clock is running at, for example `Day ratio 3.0, night ratio 1.0 (1.0 is vanilla speed)`.

## `/timescale set <day> <night>`

Sets the day ratio and the night ratio together. Both must be positive numbers; decimals such as `0.5` are allowed and speed that half of the cycle up. See [CONFIG.md](CONFIG.md) for what the ratios mean.

The new ratios take effect on the next server tick, without a restart, and are written to `day-ratio` and `night-ratio` in `plugins/Timescale/config.yml` so they survive a restart.

If either value is not a positive number, nothing is changed and the reply names the value that was rejected. A wrong number of arguments replies with `Usage: /timescale set <day> <night>`, and a sender without `timescale.set` is told which permission is missing; the permission is checked first, so such a sender gets that reply whatever arguments follow `set`.

Any first argument other than `set` (matched case-insensitively) replies with `Usage: /timescale [set <day> <night>]` and changes nothing.

## Permissions

| Permission | Default | Grants |
|---|---|---|
| `timescale.view` | `true` | `/timescale` |
| `timescale.set` | `op` | `/timescale set` |
