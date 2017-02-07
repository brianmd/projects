(ns projects.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [projects.core-test]))

(doo-tests 'projects.core-test)

