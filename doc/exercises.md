# Exercises

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

There is also `spectre.scrypt-ffi`, an alternative to `spectre.scrypt` that calls into libsodium directly via Babashka's FFI support instead of shelling out to the `openssl` binary. It is not wired in by default (`core.clj` requires `spectre.scrypt`, with the FFI require commented out above it). If you have libsodium installed and a Babashka build with FFI support, try swapping the two requires in `core.clj` and confirming the same tests still pass, i.e. the two scrypt implementations agree byte-for-byte. Each namespace also has its own `comment` block with a known input/output pair you can check directly at the REPL.

Run just this namespace while you work with `bb test --nses spectre.core-test`.

## E2

`src/spectre/clipboard.clj` is a skeleton with two functions to write:

- `tool`: return the first available clipboard command from the `tools` table already defined at the top of the file (`pbcopy`, `wl-copy`, `xclip`, `xsel`, `clip`), or `nil` if none of them exist on this machine. Use `babashka.fs/which` to check whether a command exists on the `PATH`.
- `copy!`: run the given command (defaulting to `(tool)`) and write `s` to its **stdin**.
  Use `babashka.process/process` (or `sh`) with `:in s`.
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

`src/spectre/db.clj` stores per-site settings (`:counter`, `:template`, `:variant`) in a flat EDN file, `db.edn` by default, shaped like:

```clojure
{:sites {"example.com" {:counter 1 :template :long :variant :password}}}
```

Four functions to write:

- `load-db`: read `path` and `edn/read-string` it, or return an empty db (`{:sites {}}`) when the file does not exist yet. Use `babashka.fs/exists?` to check first.
- `save-db!`: write `db` back to `path` as EDN.
- `site-settings`: look up one site's settings map in `db`, or `nil` when it is not there yet.
- `merge-site!`: merge `settings` into the existing entry for `site` (so a partial update, e.g. just a new `:counter`, does not wipe the other keys), save the result with `save-db!`, and return the updated db.

Check this exercise with `spectre.cli-test`, `spectre.tui2-test` and `spectre.tui-test`, which read `db.edn`.

## E4

`src/spectre/cli.clj` wires `spectre.core`, `spectre.clipboard`, `spectre.db` and `spectre.identicon` into the `pw` command. Four TODOs, make `test/spectre/cli_test.clj` pass:

```
bb test --nses spectre.cli-test
```

- `known-sites`: the sorted list of sites already in `db.edn`, for CLI completion. Load the db (via `spectre.db/load-db`) and pull the keys out of `:sites`.
- `spec`: currently only declares `:site` and `:print`. Add `:name`, `:counter`, `:template` and `:variant`, matching what `spec-test` expects:
  every flag needs a single-letter `:alias` (`-u`, `-c`, `-t`, `-v`) and coerces to the right type (`:counter` to a number, `:template` and `:variant` to keywords).
  For the `:site` positional, wire `known-sites` in as the completion source (see `babashka.cli`'s docs for how a spec entry offers completions).
- `site-opts`: currently just merges `defaults` with the explicit flags, ignoring the db entirely.
  Change it to read `db.edn` (`db/load-db` with the given `opts`, which carries `:path` in tests), prefer that stored setting over `defaults`, then let an explicit flag win over that.
  When the effective settings differ from what is stored (a new site, or a flag that overrides a stored value), warn on stderr and save with `db/merge-site!`.
- A template outside the known set (`spectre.core/templates`) should be rejected by the CLI parser itself: `spec-test`'s last `testing` block checks this, so make sure `:template`'s coercion/validation catches it rather than failing later inside `derive`.

## E5

Complete `spectre.tui2`, built on [charm.clj](https://github.com/TimoKramer/charm.clj), a Bubble Tea-style TUI toolkit.
See `spectre.tui` for a complete implementation of the same UI using JLine.
Complete E3 before running `spectre.tui-test`, since its search and edit tests require the database functions.

`test/spectre/tui2_test.clj` drives `tui2/update-fn` and `tui2/view` directly, so you can make all of it pass without ever running the TUI. Get there first with `bb test --nses spectre.tui2-test`, then use the REPL workflow below to see it live.

TODOs in `src/spectre/tui2.clj`:

- `matches`: filter `sites` to those containing `query`, case-insensitively, with prefix matches sorted before matches in the middle of the name (see `search-test`).
- `open-selected`: when `enter` is pressed on the search screen, switch `:mode` to `:edit`, record the selected `:site`, and load its `:draft` settings from `db/site-settings` when the site is already in `db.edn`, falling back to `defaults` for a new one (see `edit-test`).
- `cycle-value`: step to the next or previous value in `values`, wrapping around at either end.
- `adjust`: use `cycle-value` for fields that declare `:values` (`template`, `variant`); for `:counter`, increment or decrement, never going below 1.
- `figure` (optional, for whoever is done early): the identicon (`spectre.identicon/identicon-of`) for whatever is currently typed into `:name-input`/`:master-input` on the identity screen, or `nil` while either is still empty.
  `figure-test` checks it updates live as you type and that the search screen picks up the same figure once it exists.

### Change the TUI while it runs

`bb tui2 --nrepl` starts an nREPL server on port 1667, then starts the TUI.
Connect your editor to that port. Then you can write E5 without a restart.

Do this smoke test before you write any code:

1. Start the TUI with `bb tui2 --nrepl`.
1. Connect your editor to port 1667.
1. Open `src/spectre/tui2.clj`. In `search-view`, change the text `"no sites in db.edn yet"` to `"REPL WORKS"`. Then evaluate the whole `search-view` form.
1. Press a key in the TUI. The line below the search prompt shows the new text. The rest of the screen does not change.
1. Undo the change. Then evaluate the form again.

If the text does not appear, your editor is connected to another process.

Notes:

- A new definition appears on the next message. Evaluate, then press a key.
- Do not print to `*out*` from the REPL. The text writes over the TUI display.
- E3 gives you `spectre.db/load-db`. Before E3 is done, the TUI shows
  "no sites in db.edn yet" and the list is empty.

## E6

Package and run the CLI.

- Install [bbin](https://github.com/babashka/bbin) if you have not already.
  Create `deps.edn` with `:paths ["src"]` and the `charm.clj` dependency.
  Run:

  ```sh
  bbin install . --as pw --main-opts '["-m" "spectre.cli/-main"]'
  ```

  This installs `-main`, which goes through `cli/dispatch` and gives you `pw --help`.
  Run `pw --help` and `pw example.com` from another directory.

  To store the script name and options, add `:bbin/bin {pw {:main-opts ["-m" "spectre.cli/-main"]}}` to `bb.edn`.
  Then run `bbin install .`.
- Create a script directory with its own `bb.edn`.
  A `bb.edn` next to the invoked file is respected, see [Script-adjacent bb.edn](https://book.babashka.org/#_script_adjacent_bb_edn) in the babashka book.
  Point `:paths` at this repo's `src` and add the dependencies.
  Write a script that calls `spectre.cli/generate` with a fixed site.
  Run it with `bb`.
