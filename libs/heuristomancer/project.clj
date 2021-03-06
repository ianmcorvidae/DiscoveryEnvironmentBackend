(defproject org.iplantc/heuristomancer "5.0.0"
  :description "Clojure library for attempting to guess file types."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :profiles {:dev {:resource-paths ["test-data"]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [instaparse "1.2.1"]]
  :plugins [[org.iplantc/lein-iplant-cmdtar "5.0.0"]]
  :aot [heuristomancer.core]
  :main heuristomancer.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
