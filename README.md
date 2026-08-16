# Babashka Workshop - Spectre

This is a workshop which uses Babashka to implement a password manager CLI which:

- Implements the stateless password algorithm [Spectre](https://spectre.app/). More details [here](https://spectre.app/spectre-algorithm.pdf).
- Has nicer UX of saving the site metadata locally which can be handled via an intuitive TUI.
- Has an ergonomic CLI.
- Fast enough and extensible.

## Prerequsites

The following must the installed:

- [Babashka](https://github.com/babashka/babashka#installation) (latest version preferred).
- OpenSSL 3+
  - Linux: Install the latest using your favourite package manager.
  - Mac: `brew install openssl` this MUST be used as the inbuilt one is not compatible.
  - Windows 10+: `winget install openssl`

## License

Copyright © Michiel Borkent and Rahul De
License: [MIT](https://opensource.org/license/mit). See LICENSE.
