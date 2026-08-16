# Babashka Workshop - Spectre

This is a workshop which uses Babashka to implement a password manager CLI which:

- Implements the stateless password algorithm [Spectre](https://spectre.app/). More details [here](https://spectre.app/spectre-algorithm.pdf).
- Has nicer UX of saving the site metadata locally which can be handled via an intuitive TUI.
- Has an ergonomic CLI.
- Fast enough and extensible.

## Prerequsites

The following must the installed:

- [Babashka](https://github.com/babashka/babashka#installation) (latest version preferred).
- Java 17+ (latest preferred).
- Optional: Clojure [CLI](https://clojure.org/guides/install_clojure)
- OpenSSL 3+
  - Linux: Install the latest using your favourite package manager.
  - Mac: `brew install openssl` this MUST be used as the inbuilt one is not compatible.
  - Windows 10+: `winget install openssl`

### Editor setup

To have the optimal dev experience we recommend the following to be setup

- Clojure [LSP](https://clojure-lsp.io/installation/)
- Some form of structural editing mode like Paredit or Parinfer.
- Neovim: https://github.com/Olical/conjure
- Emacs: https://docs.cider.mx/cider/index.html
- IntelliJ IDEA: https://cursive-ide.com/
- VSCode/Codium: https://calva.io/
- Vim: https://github.com/liquidz/elin (Also written in babashka!)
- Sublime Text 4: https://github.com/tonsky/Clojure-Sublimed

## License

Copyright © Michiel Borkent and Rahul De

License: [MIT](https://opensource.org/license/mit). See LICENSE.
