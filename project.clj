(defproject steno "0.1.0-dev06"
  :description "Digitize stenographic writings"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [expound "0.7.1"]
                 [com.rpl/specter "1.1.1"]
                 [net.mikera/imagez "0.12.0"]
                 [org.boofcv/boofcv-core "0.30"]
                 [org.boofcv/boofcv-swing "0.30"]]

  :plugins [[lein-ancient "0.6.10" :exclusions [commons-logging org.clojure/clojure]]
            [jonase/eastwood "0.2.6-beta2"]
            [lein-kibit "0.1.6" :exclusions [org.clojure/clojure]]
            [lein-cljfmt "0.5.7" :exclusions [org.clojure/clojure org.clojure/clojure rewrite-clj]]
            [lein-codox "0.10.3" :exclusions [org.clojure/clojure]]
            [lein-environ "1.1.0"]]
  :repl-options {:init-ns user}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :profiles {:check {:global-vars {*warn-on-reflection* true}}
             :dev {:source-paths   ["dev/src"]
                   :resource-paths ["dev/resources"]}}
  :pom-addition [:developers [:developer
                              [:name "Dan Pomohaci"]
                              [:email "dan.pomohaci@gmail.com"]
                              [:timezone "+3"]]]
  :codox {:doc-files []
          :exclude-vars nil
          :project {:name "steno"}
          :source-paths ["src"]
          :output-path "docs/api"})
