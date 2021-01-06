# Go Colonize

![Java CI with Gradle](https://github.com/Virtlink/go-colonize/workflows/Java%20CI%20with%20Gradle/badge.svg)
![License](https://img.shields.io/github/license/virtlink/go-colonize)

A tool for adding semi-colons to Go source code.

The Go formal grammar uses semi-colons to terminate statements for disambiguation. For example, `f()(g())` calls the result of function `f()` with argument `g()`, whereas `f();(g())` is simply two function calls, the latter of which is parenthesized.

To avoid having to write semi-colons everywhere, Go allows them to be elided at the end of lines and before closing
curly braces.[1] To enable a parser to parse Go efficiently, this tool re-inserts these missing semi-colons.

## Usage
Specify an input file and output file:

    ./colonize myfile.go -o myoutput.go

If the output is omitted, the result will be output on standard out.
To read the input from the standard input, specify `-` as the input file.

## TODO
- [ ] Colonize statements before closing brackets
 

## License
Copyright 2020-2021 Daniel A. A. Pelsmaeker

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an **"as is" basis, without warranties or conditions of any kind**, either express or implied. See the License for the specific language governing permissions and limitations under the License.

This project includes source code from other projects:

- [Golang Antlr Grammar][2] ([BSD-3][3] license) 

[1]: https://golang.org/doc/effective_go.html#semicolons
[2]: https://github.com/antlr/grammars-v4/tree/master/golang
[3]: https://opensource.org/licenses/BSD-3-Clause