(defproject whitesnake "0.1.0-SNAPSHOT"
  :description "An imutable database"
  :url "http://github.com/Turtel216/whitesnake"
  :license {:name "MIT License"
            :url "https://github.com/Turtel216/whitesnake/main/LICENSE"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot whitesnake.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
