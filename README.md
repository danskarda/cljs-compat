# cljs-compat

A Clojure library which provides thin compatibility layer for Clojure
and ClojureScript. It hides implementation details which make hard
to share complex code between Clojure and ClojureScript.

## Introduction

Clojure and ClojureScript are hosted languages. Clojure is hosted on
Java Virtual Machine, ClojureScript is hosted on JavaScript. The
differences in implementation and hosting platforms make it challanging
to write platform independent code. 

To write plaform independent code you can hit four speed bumps:

1. different naming conventions of core types and protocols
2. different syntax related to macros and namespaces
3. differences imposed by hosting platform (JVM and JavaScript)
4. different set of libraries

cljs-compat provides tools to deal with first two types of differences.
There is very little it can do about 3-4.

### in-lang, in-clojure, in-clojurescript

`cljs-compat.macro` provides several macros which execute code only
in particular language. First let's start with `project.clj`:

```clj
(defproject my.foo.project
   :dependencies [[cljs-compat "0.1.0"]])
```

`cljs-compat.macro` provides following macros:

* `in-lang` with keyword parameters `:clj`, `:cljs`, `:clojure` and `:clojurescript`
* `in-clj` and `in-clojure` macro
* `in-cljs` and `in-clojurescript` macro

```clj
(ns my.foo.bar
  (:require [[cljs-compat.macro :refer [in-lang in-clojure in-clojurescript]]]))

;;; evals to "Hello, Clojure!"
(in-lang
  :clj  		  "Hello, Clojure!"	
  :cljs           "Hello, ClojureScript")

;;; evals to "Hello, Clojure"
(in-clojure
  "Hello, Clojure!")

;;; evals to nil
(in-clojurescript
  "Hello, ClojureScript!")
```

For ClojureScript these macros are implemented in `cljs-compat.macro-cljs` namespace.

```clj
(ns my.foo.cljs
  (:require-macro [[cljs-compat.macro-cljs :refer [in-lang in-clojure in-clojurescript]]]))

;;; evals to "Hello, ClojureScript!"
(in-lang
  :clj  		  "Hello, Clojure!"	
  :cljs           "Hello, ClojureScript")

;;; evals to nil
(in-clojure
  "Hello, Clojure!")

;;; evals to "Hello, ClojureScript!"
(in-clojurescript
  "Hello, ClojureScript!")
```

With [lein-cljsbuild][cljsbuild] you can use crossovers:

```clj
(ns my.foo.crossover
   (:require;*CLJSBUILD-REMOVE*;-macros
     [[cljs-compat.macro;*CLJSBUILD-REMOVE*;-cljs
	      :refer [in-lang in-clojure in-clojurescript]]]))
```

This is not particulary nice prelude, but we can improve that later with
`cljs-compat-crossover`.

### deftype, defrecord, extend-type, extend-protocol

Second biggest hurdle in writing portable code is naming of core
types, protocols and hints. Clojure code:

```clj
(deftype FooType [^:volatile-mutable bar]
   java.lang.Object
   (equals [o1 o2]
     ...)

   clojure.lang.ILookup
   (valAt [foo key not-found]
      ...))
```

and in ClojureScript:
```clj
(deftype FooType [^:mutable bar]
   IEquiv
   (-equiv [o1 o2]
     ...)

   ILookup
   (-lookup [foo key not-found]
      ...))
```

You got the point. Luckily number and order of parameters is the same.

`cljs-core.macro-cljs` provides its own versions of `deftype`,
`defrecord`, `extend-type` and `extend-protocol` macros. These macros
translate all core protocol and method names and update hints. 

See [protocols.clj][compat/src/cljs_compat/protocols.clj] for detailed
mapping.

```clj
(ns my.foo.types
   (:refer-clojure :exclude [deftype ...])
   (:require-macros
     [[cljs-compat.macro-cljs :refer [deftype ...]]]))
```

or in a standard crossover file:
```clj
(ns my.foo.types
   (:refer-clojure :exclude [deftype ...])
   (:require;*CLJSBUILD-REMOVE*;-macros
     [[cljs-compat.macro;*CLJSBUILD-REMOVE*;-cljs
	    :refer [deftype ...]]]))
```

Again, standard cljsbuild crossovers are not nice, but in a minute, we
can do much better.

### cljs-compat-crossover

The difference in `ns` syntax is the most difficult to overcome. `ns`
is evaluated before anything is loaded into namespace. We cannot use
macros directly to transform the code in crossover files.

[lein-cljsbuild][cljsbuild] provides crossovers to deal with the
platform specific code. Unfortunatelly standard crossovers are
implemented as a text preprocessor, replacing regular expressions
line-by-line.

To improve the situation, I implemented a patch to enable arbitrary
crossover plugins (see [danskarda/lein-cljsbuild][cljsbuild-patched]).
At this moment the patch is not merged to mainline. If you feel
brave enough, you can download and try manually.

`cljs-compat-crossover` is an implementation of a crossover plugin.
It transforms the code as clojure expressions:

```clj
(defproject my.project
  :dependencies			[[cljs-compat			"0.1.0"]
						 [cljs-compat-crossover	"0.1.0"]]
  :plugins				[[lein-cljsbuild		"0.3.2"]]

  :cljsbuild
  {:crossover-transform  cljs-compat.crossover/conservative
```

At this moment, there are two functions for crossover tranformation:
conservative and progressive.

`cljs-compat.crossover/conservative`:

* Automagically transforms `:use` and `:require` to `:use-macros` and
  `:require-macros`, if required namespace ends with .macro
  
* Adds -cljs suffix to macro namespace, iff the file exists.

These two additions help us to simplify crossover code above:

```clj
(ns my.foo.crossover
  (:require [[cljs-compat.macro :refer [in-lang]])))
```

`cljs-compat.crossover/progressive` (or rather aggressive) performs
conservtive plus several other changes:

* inject cljs-compat.macros into ns definition if it is not already
  included.
  
* transform basic types, exceptions and vars

  `clojure.lang.PersistentHashMap` to `cljs.lang/PersistentHashMap`,
  `java.lang.Error` to `js/Error`,
  `clojure.lang.PersistentQueue/EMPTY` to
  `cljs.core.PersistentQueue/EMPTY`, `(java.util.Date.)` to
  `(js/Date.) etc

* Replace clojure.test dependency with cemerick.cljs.test

### REPL integration

lein-cljsbuild helps with compilation of crossover files. If you
want to execute same code in REPL, you might run into trouble.

[Piggieback][piggie] is a nrepl middleware which can execute
ClojureScript code in Rhino or in the browser.

`cljs-compat-crossover` includes a support for piggieback, which
applies same transformations on code executed in repl (piggieback)
as code compiled in crossovers.

```clj
(defproject my.foo.project
   :dependencies [[cljs-compat-crossovers "0.1.0"]]
   
   :repl-options
   {:nrepl-middleware [cljs-compat.piggieback/wrap-progressive-repl]})
   
   ...
```

and then you start piggieback ClojureScript session from REPL as you
would with original piggieback. See [piggieback][piggie] documentation
for details.

### Hosting platform

For the differences in hosting platforms, there is very little
cljs-compat can do. Clojure and ClojureScript were both designed
to be hosted languages.

The aim of `cljs-compat` is to provide a cure only for differences in
names and syntax.

You have to keep in mind that Java and JavaScript have
different core types. Sometimes this might have surprising effects,
for example:

```clj
(< 1 nil)
;;; throws exception in Clojure, returns true in ClojureScript

(.-foo bar)
;;; throws exception in Clojure (if slot foo does not exists in bar)
;;; returns nil in ClojureScript (if bar is an object)
```

There is nothing cljs-compat can do about this kind of differences.
You have to keep them in mind while coding platform independent code.

[cljsbuild]:			https://github.com/emezeske/lein-cljsbuild/
[cljsbuild-patched]:	https://github.com/danskarda/lein-cljsbuild/
[piggie]:				https://github.com/cemerick/piggieback


## License

Copyright © 2013 Daniel Skarda

Distributed under the Eclipse Public License, the same as Clojure.


