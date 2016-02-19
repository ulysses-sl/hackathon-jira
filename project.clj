(defproject hacka "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.google.api-client/google-api-client "1.21.0"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.21.0"]
                 [com.google.apis/google-api-services-gmail "v1-rev37-1.21.0"]
                 [http-kit "2.1.18"]]
  :main ^:skip-aot hacka.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
