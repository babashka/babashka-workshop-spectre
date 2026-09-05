# Babashka Workshop - Spectre

This workshop uses Babashka to build a password manager CLI with:

- The stateless [Spectre password algorithm](https://spectre.app/). See the [algorithm description](https://spectre.app/spectre-algorithm.pdf).
- Local storage for site settings, with a terminal UI for editing them.
- A command-line interface for generating passwords.
- Babashka extensions for native scrypt implementations.

## Prerequisites

Install the following:

- [Babashka](https://github.com/babashka/babashka#installation), with FFI support if you want to use the libsodium implementation (>= 1.13.220).
  - Make sure to set up completions with:
    `source <(bb org.babashka.cli/completions snippet --shell zsh --prog bb)`.
    See https://github.com/babashka/cli#completions for your specific shell.
- Java 22 or later (17 works fine, but 22 is needed for FFI).
- [Clojure CLI](https://clojure.org/guides/install_clojure) (optional).
- OpenSSL 3 or later, unless you use the FFI implementation.
- [libsodium](https://libsodium.gitbook.io/doc/installation) for the FFI implementation

On macOS, install OpenSSL with `brew install openssl`. The system OpenSSL version is not compatible.

On Windows 10 or later, install OpenSSL with `winget install openssl`.

### Editor setup

For editor support, install:

- Clojure [LSP](https://clojure-lsp.io/installation/)
- A structural editing mode such as Paredit or Parinfer.
- Neovim: https://github.com/Olical/conjure
- Emacs: https://docs.cider.mx/cider/index.html
- IntelliJ IDEA: https://cursive-ide.com/
- VSCode/Codium: https://calva.io/
- Vim: https://github.com/liquidz/elin
- Sublime Text 4: https://github.com/tonsky/Clojure-Sublimed

## License

Copyright (c) Michiel Borkent and Rahul De

License: [MIT](https://opensource.org/license/mit). See LICENSE.
