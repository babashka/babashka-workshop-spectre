# Exercises

TODO:

- MB: write slides for walkthrough of core libs (including FFI) in pitch deck
- RD: write elaborate instructions here for all exercises

## E1

Write a few sanity tests for core and run with bb test task:

TODO: how to run one test, or one namespace, using bb test --nses ...

- E1: Empty project with specter.scrypt/core given + cognitect test runner + test skeletons
  - Write a few sanity tests for core and run with bb test task

## E2

- E2: clipboard skeleton

## E3

- E3: specter.db skeleton

## E4

- E4: specter.cli skeleton

4 TODOs, make tests pass

## E5

- E5: specter.tui skeleton

### Change the TUI while it runs

`bb tui2 --nrepl` starts an nREPL server on port 1667, then starts the TUI.
Connect your editor to that port. Then you can write E5 without a restart.

Do this smoke test before you write any code:

1. Start the TUI with `bb tui2 --nrepl`.
2. Connect your editor to port 1667.
3. Open `src/spectre/tui2.clj`. In `search-view`, change the text
   `"no sites in db.edn yet"` to `"REPL WORKS"`. Then evaluate the whole
   `search-view` form.
4. Press a key in the TUI. The line below the search prompt shows the new
   text. The rest of the screen does not change.
5. Undo the change. Then evaluate the form again.

If the text does not appear, your editor is connected to another process.

Notes:

- A new definition appears on the next message. Evaluate, then press a key.
- Do not print to `*out*` from the REPL. The text writes over the TUI display.
- E3 gives you `spectre.db/load-db`. Before E3 is done, the TUI shows
  "no sites in db.edn yet" and the list is empty.

## E6

- E6: bbin (no skeleton, just follow bbin docs). Also show local script with relative bb.edn as lighter weight local solution.
