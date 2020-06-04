# Go Colonize

![Java CI with Gradle](https://github.com/Virtlink/go-colonize/workflows/Java%20CI%20with%20Gradle/badge.svg)
![License](https://img.shields.io/github/license/virtlink/go-colonize)

A tool for adding semi-colons to Go source code.

The Go formal grammar uses semi-colons to terminate statements for disambiguation. For example, `f()(g())` calls the result of function `f()` with argument `g()`, whereas `f();(g())` is simply two function calls, the latter of which is parenthesized.

To avoid having to write semi-colons everywhere, Go allows them to be elided at the end of lines and before closing
curly braces.[1] To enable a parser to parse Go efficiently, this tool re-inserts these missing semi-colons.



[1]: https://golang.org/doc/effective_go.html#semicolons