# The village 

Maybe a city builder, not really something yet, you can
go back where you came from.

## Contribute

> for my future self

The build relies on CLI tools.
The code must be cljc, I want it to run on JVM and JS.

### Dev on the web

Run `clojure -Acljs:test:figwheel`.

A page at [http://localhost:9500/]() opens. That's the web
app (probably nothing much yet). Code and CSS must be 
hot reloaded.

Tests can be run at [http://localhost:9500/figwheel-extra-main/auto-testing]().
It relies on figwheel auto testing.

You can interact with the app thanks to the REPL that just
launched with the whole thing.

## Dev on the JVM

There is no GUI (yet). Just unit tests.

Run `clojure -Atest`.