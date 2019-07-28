(ns the-village.run-all
  (:require [cljs.test :refer-macros [run-all-tests]]
            [the-village.engine.storage-seq-test]))

(run-all-tests)