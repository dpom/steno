(defproject steno "0.1.0-dev10"
  :description "Digitize stenographic writings"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [clj-time "0.15.0"]
                 [expound "0.7.1"]
                 [com.rpl/specter "1.1.1"]
                 [com.hypirion/clj-xchart "0.2.0"]
                 [net.mikera/imagez "0.12.0"]
                 [duct/core "0.6.2"]
                 [duct/module.logging "0.3.1"]
                 [duct/module.web "0.6.4"]
                 [duct/module.ataraxy "0.2.0"]
                 [duct/module.sql "0.4.2"]
                 [org.postgresql/postgresql "42.1.4"]]

  :plugins [[lein-ancient "0.6.10" :exclusions [commons-logging org.clojure/clojure]]
            [jonase/eastwood "0.2.6-beta2"]
            [lein-kibit "0.1.6" :exclusions [org.clojure/clojure]]
            [lein-cljfmt "0.5.7" :exclusions [org.clojure/clojure org.clojure/clojure rewrite-clj]]
            [lein-codox "0.10.3" :exclusions [org.clojure/clojure]]
            [lein-environ "1.1.0"]
            [duct/lein-duct "0.10.6"]]
  :main ^:skip-aot steno.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
 
  :profiles {:check {:global-vars {*warn-on-reflection* true}}
             :repl {:prep-tasks   ^:replace ["javac" "compile"]
                    :repl-options {:init-ns user}}
             :uberjar {:aot :all}
             :dev {:source-paths   ["dev/src"]
                   :resource-paths ["dev/resources"]
                   :dependencies   [[integrant/repl "0.2.0"]
                                    [eftest "0.4.1"]
                                    [kerodon "0.9.0"]]}}
  :pom-addition [:developers [:developer
                              [:name "Dan Pomohaci"]
                              [:email "dan.pomohaci@gmail.com"]
                              [:timezone "+3"]]]
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :codox {:doc-files []
          :exclude-vars nil
          :project {:name "steno"}
          :source-paths ["src"]
          :output-path "docs/api"})
