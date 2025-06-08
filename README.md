# Whitesnake

> [!CAUTION]
> The application is still in early development. Many features are not tested or yet to be implemented.

**Whitesnake** is a functional, immutable database implemented in [Clojure](https://clojure.org/). It emphasizes simplicity, composability, and a pure functional approach to data storage and querying.

Whitesnake is currently under active development and is built with [Leiningen](https://leiningen.org/) as the build and dependency management tool.

---

## Features

- 🧠 **Purely Functional Core** — No mutations, side effects, or global state.
- 🔁 **Immutable Data** — Data is versioned and persistent, enabling rollback, history inspection, and easy concurrency.
- 🔎 **Query Engine** — Expressive and composable query system inspired by datalog.
- ⚙️ **Clojure-native** — Designed to feel idiomatic and work seamlessly with the Clojure ecosystem.
- 🧪 **Testable** — Every part of the system is designed to be unit-testable and side-effect-free.

---

## Getting Started

### Prerequisites

- [Clojure](https://clojure.org/guides/getting_started)
- [Leiningen](https://leiningen.org/)

### Installation

Clone the repository:

```bash
git clone https://github.com/Turtel216/whitesnake.git
cd whitesnake
````

Run the REPL:

```bash
lein repl
```

---

## Usage

Here’s a quick taste of how to define a database, insert data, and run a query:

```clojure
(ns example.core
  (:require [whitesnake.core :as ws]))

(def db (ws/empty-db))

(def db2 (-> db
             (ws/insert {:id 1 :name "Alice" :role :engineer})
             (ws/insert {:id 2 :name "Bob" :role :designer})))

(ws/query db2 {:where [[:?e :name "Alice"] [:?e :role :engineer]]})
```

---

## Project Structure

* `src/whitesnake/core.clj` – Main database implementation.
* `test/whitesnake/core_test.clj` – Unit tests.
* `project.clj` – Leiningen project configuration.

---

## Development

Run tests:

```bash
lein test
```

Format code (if using cljfmt or similar tools):

```bash
lein cljfmt fix
```

---

## Roadmap

* [ ] Transaction logs and time-travel queries
* [ ] Query optimizer
* [ ] More expressive query language
* [ ] CLI or web REPL interface
* [ ] Persistence layer (disk, remote storage)

---

## Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check [issues](https://github.com/Turtel216/whitesnake/issues) and submit a PR.

---

## License

MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgments

Inspired by:

* [Datascript](https://github.com/tonsky/datascript)
* [Datomic](https://www.datomic.com/)
* The Clojure philosophy and ecosystem
