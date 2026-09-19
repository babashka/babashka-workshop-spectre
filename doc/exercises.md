# Exercises

Tests fail until you complete the exercises. They use temporary files and in-memory data.

Run all tests at any point with:

```
bb test
```

To run a single namespace instead of the whole suite, pass `--nses`:

```
bb test --nses spectre.core-test
```

`--nses` takes any number of symbols, so you can list several namespaces at once. To narrow further, to one or a few `deftest` vars, use `--vars` instead (or together with `--nses`):

```
bb test --vars spectre.cli-test/spec-test
```

You can also select by test metadata with `--includes`/`--excludes`, e.g. `bb test --excludes :clipboard` to skip the real-clipboard test in E2.

## E1

`spectre.core` and `spectre.scrypt` are given to you complete and working:
together they are a full, correct implementation of the Spectre v3 algorithm.
Read through `src/spectre/core.clj` before doing anything else, it is the foundation every other exercise builds on.

`test/spectre/core_test.clj` has only one test, checking a single known name/password/site combination against its expected output. Add a few more sanity tests of your own to that namespace:

- Derive a password for the same name and master password but a different `site`, and check it differs from the existing example.
- Derive with a different `variant` (`:login`, `:answer`) and check that changes the result too.
- Derive with a different `template` (e.g. `:maximum`, `:pin`, `:short`) and check the length/shape looks right for that template.
- Bump `:counter` and check you get a different password again.
- Check that two calls with the same arguments return the same password.

There is also `spectre.scrypt-ffi`, an alternative to `spectre.scrypt` that calls into libsodium directly via Babashka's FFI support instead of shelling out to the `openssl` binary. It is not wired in by default (`core.clj` requires `spectre.scrypt`, with the FFI require commented out below it). If you have libsodium installed and a Babashka build with FFI support, try swapping the two requires in `core.clj` and confirming the same tests still pass, i.e. the two scrypt implementations agree byte-for-byte. Each namespace also has its own `comment` block with a known input/output pair you can check directly at the REPL.

Run just this namespace while you work with `bb test --nses spectre.core-test`.

## E2

`src/spectre/clipboard.clj` is a skeleton with two functions to write:

- `tool`: return the first available clipboard command from the `tools` table already defined at the top of the file (`pbcopy`, `wl-copy`, `xclip`, `xsel`, `clip`), or `nil` if none of them exist on this machine. Use `babashka.fs/which` to check whether a command exists on the `PATH`.
- `copy!`: run the given command (defaulting to `(tool)`) and write `s` to its **stdin**.
  Use `babashka.process/shell` (or `sh`) with `:in s`. `process` does not wait for the command to exit.
  Return the command used, or `nil` when there is none to fall back to.

`test/spectre/clipboard_test.clj` drives `copy!` with a fake "clipboard" command that just writes stdin to a file, so `copy-test` and `no-tool-test` run everywhere without touching your real clipboard.
Get those two passing first:

```
bb test --nses spectre.clipboard-test --excludes :clipboard
```

There is a third test, `real-clipboard-test`, tagged `^:clipboard`, that exercises your actual system clipboard. It only runs when `SPECTRE_CLIPBOARD_TEST` is set.

```
SPECTRE_CLIPBOARD_TEST=1 bb test --nses spectre.clipboard-test
```

## E3

`src/spectre/db.clj` stores per-site settings (`:counter`, `:template`, `:variant`) in an EDN file, shaped like:

```clojure
{:sites {"example.com" {:counter 1 :template :long :variant :password}}}
```

`default-path` is `~/.config/spectre/db.edn`. Set `SPECTRE_DB` to override it.

TODOs, make `test/spectre/db_test.clj` pass:

```
bb test --nses spectre.db-test
```

- `load-db`: when `path` exists, read it and `edn/read-string` it. Use `babashka.fs/exists?` to check first.
- `save-db!`: create the parent directory of `path` with `fs/create-dirs`, then write `db` back to `path` as EDN.
- `site-settings`: look up one site's settings map in `db`, or `nil` when it is not there yet.
- `merge-site!`: merge `settings` into the existing entry for `site` (so a partial update, e.g. just a new `:counter`, does not wipe the other keys), save the result with `save-db!`, and return the updated db.

## E4

`src/spectre/cli.clj` wires `spectre.core`, `spectre.clipboard`, `spectre.db` and `spectre.identicon` into the `pw` command.

TODOs, make `test/spectre/cli_test.clj` pass:

```
bb test --nses spectre.cli-test
```

- `known-sites`: the sorted list of sites already in `db.edn`, for CLI completion. Load the db (via `spectre.db/load-db`) and pull the keys out of `:sites`.
- The CLI `spec`: currently only declares `:site` and `:print`. Add `:name`, `:counter`, `:template` and `:variant`, matching what `spec-test` expects:
  every flag needs a single-letter `:alias` (`-u`, `-c`, `-t`, `-v`) and coerces to the right type (`:counter` to a number, `:template` and `:variant` to keywords).
  For the `:site` positional, wire `known-sites` in as the spec entry's `:complete-fn`.
- `site-opts`: currently just merges `defaults` with the explicit flags, ignoring the db entirely.
  Change it to read `db.edn` (`db/load-db` with the given `opts`, which carries `:path` in tests), prefer that stored setting over `defaults`, then let an explicit flag win over that.
  When the effective settings differ from what is stored (a new site, or a flag that overrides a stored value), warn on stderr and save with `db/merge-site!`.
- Reject unknown templates in the CLI parser. Use `:coerce :keyword` and `:validate` with the set of keys from `spectre.core/templates`. See `spec-test`.

## E5

Complete `spectre.tui2`, built on [charm.clj](https://github.com/TimoKramer/charm.clj), a Bubble Tea-style TUI toolkit.
See `spectre.tui` for a complete implementation of the same UI using JLine.
Complete E3 first. The seed task and TUI use its database functions.

Add example sites for searching and editing:

```sh
bb db:seed
```

The task adds missing entries for `google.com`, `mail.google.com` and `example.org`
to `SPECTRE_DB` or the default database. Existing settings stay unchanged.

Run the REPL smoke test below before filling in the TODOs.
Then use `bb test --nses spectre.tui2-test --excludes :optional` while you work.
The tests call `tui2/update-fn` and `tui2/view` directly. They do not require seeding.

TODOs in `src/spectre/tui2.clj`:

- `matches`: filter `sites` to those containing `query`, case-insensitively, ranked by where the query appears in the name: a hit at the start comes before a hit further along (see `search-test`, which also asks you to add one assertion of your own on `matches`).
- `open-selected`: on Enter, set `:mode` to `:edit` and `:site` to the selected site. Set `:draft` to `defaults` merged with `db/site-settings`. With no selection, leave the state unchanged. See `edit-test`.
- `cycle-value`: step to the next or previous value in `values`, wrapping around at either end. A value that is not in `values` starts at the first one (see `cycle-value-test`).
- `adjust`: use `cycle-value` for fields that declare `:values` (`template`, `variant`); for `:counter`, increment or decrement, never going below 1.
- `figure` (optional, for whoever is done early): the identicon (`spectre.identicon/identicon-of`) for whatever is currently typed into `:name-input`/`:master-input` on the identity screen, or `nil` while either is still empty.
  `figure-test` checks it updates live as you type and that the search screen picks up the same figure once it exists.

Run the optional `figure-test` with
`bb test --nses spectre.tui2-test` when you complete `figure`.
Use `bb test --excludes :optional` to check all required exercises.

After completing the TODOs, select a site, change its counter and press Enter to save.
Reopen it to check the saved value. Use the CLI or `bb db:seed` to add sites.

### Change the TUI while it runs

`bb tui2 --nrepl` starts an nREPL server on port 1667, then starts the TUI.
Connect your editor to that port. Then you can write E5 without a restart.

Do this smoke test before you write any code:

1. Start the TUI with `bb tui2 --nrepl`.
1. Connect your editor to port 1667.
1. Open `src/spectre/tui2.clj`. In `search-view`, insert `"REPL WORKS\n"` as the first argument of the outer `str` call. Then evaluate the whole `search-view` form.
1. Press a key in the TUI. The new text appears above the search prompt.
1. Undo the change. Then evaluate the form again.

If the text does not appear, check that evaluation succeeded and your editor is connected to port 1667.

Notes:

- A new definition appears on the next message. Evaluate, then press a key.
- Do not print to `*out*` from the REPL. The text writes over the TUI display.
- E3 gives you `spectre.db/load-db`. Before E3 is done, the TUI shows
  "no sites in db.edn yet" and the list is empty.

## E6

Run the CLI from another directory using each of these methods.

- Create a launcher that uses this repo's `bb.edn`.
  `--config` resolves `:paths` and `:deps` relative to that file.

  On macOS or Linux, save this as `pw` in a directory on your `PATH` and run `chmod +x` on the file:

  ```sh
  #!/bin/sh
  bb --config "/path/to/babashka-workshop-spectre/bb.edn" -m spectre.cli/-main "$@"
  ```

  On Windows, save this as `pw.cmd` in a directory on your `PATH`:

  ```bat
  @echo off
  bb --config "C:\path\to\babashka-workshop-spectre\bb.edn" -m spectre.cli/-main %*
  ```

  Replace the config path with your checkout's path.
  You can also use the `pw` task instead of `-m spectre.cli/-main`.
  This example uses `-main` to match the bbin configuration below.
  Test with `pw --help` and `pw example.com` from another directory.

- Install a launcher with [bbin](https://github.com/babashka/bbin).
  Create `deps.edn` in this repo with `:paths ["src"]` and the `charm.clj` dependency from `bb.edn`.
  bbin uses `deps.edn` to resolve the repo as a `:local/root` dependency.
  Add this entry to the repo's `bb.edn`:

  ```clojure
  :bbin/bin {pw {:main-opts ["-m" "spectre.cli/-main"]}}
  ```

  Install `pw` from the repo directory:

  ```sh
  bbin install .
  ```

  On macOS or Linux, check with `command -v pw` that the bbin launcher is on PATH
  before the manual launcher.

  Test with `pw --help` and `pw example.com` from another directory.

## Optional open-ended exercises

- Switch the db to sqlite using [babashka.sqlite](https://github.com/babashka/babashka.sqlite)
- Make the tests run on Github Actions using [setup-clojure](https://github.com/DeLaGuardo/setup-clojure)
