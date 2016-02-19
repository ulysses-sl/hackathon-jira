(ns hacka.core
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [org.httpkit.client :as http]
    [clojure.data.json :as json]))

(import com.google.api.client.auth.oauth2.Credential
        com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
        com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
        com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
        com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow$Builder
        com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
        com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
        com.google.api.client.http.HttpTransport
        com.google.api.client.json.jackson2.JacksonFactory
        com.google.api.client.json.JsonFactory
        com.google.api.client.util.store.FileDataStoreFactory)

(import com.google.api.services.gmail.GmailScopes
        '(com.google.api.services.gmail.model
           Draft
           History
           HistoryLabelAdded
           HistoryLabelRemoved
           HistoryMessageAdded
           HistoryMessageDeleted
           Label
           ListDraftsResponse
           ListHistoryResponse
           ListLabelsResponse
           ListMessagesResponse
           ListThreadsResponse
           Message
           MessagePart
           MessagePartBody
           MessagePartHeader
           ModifyMessageRequest
           ModifyThreadRequest
           Profile
           WatchRequest
           WatchResponse)
        com.google.api.services.gmail.Gmail
        com.google.api.services.gmail.Gmail$Builder)

(import java.io.InputStreamReader
        java.util.Arrays
        java.util.ArrayList
        java.util.List)

(def appname "Hackathon")

(def data-store-dir (java.io.File. (System/getProperty "user.home") ".credentials/"))

(def json-factory (JacksonFactory/getDefaultInstance))

(def scopes (ArrayList. [GmailScopes/MAIL_GOOGLE_COM]))

(def http-transport (GoogleNetHttpTransport/newTrustedTransport))

(def data-store-factory (FileDataStoreFactory. data-store-dir))

(defn authorize
  []
  (let [client-secret-reader (-> "client_secret.json" (io/resource) (io/input-stream) (io/reader))
        client-secrets (GoogleClientSecrets/load json-factory client-secret-reader)
        flow (-> (GoogleAuthorizationCodeFlow$Builder. http-transport json-factory client-secrets scopes)
                 (.setDataStoreFactory data-store-factory)
                 (.setAccessType "offline")
                 (.build))
        credential (.authorize (AuthorizationCodeInstalledApp. flow (LocalServerReceiver.)) "user")]
    (do
      (println (clojure.string/join ["Credentials saved to " (.getAbsolutePath data-store-dir)]))
      credential)))

(defn getGmailService
  []
  (-> (Gmail$Builder. http-transport json-factory (authorize))
      (.setApplicationName appname)
      (.build)))

(defn unhexify [s]
  (let [bytes (into-array Byte/TYPE
                 (map (fn [[x y]]
                    (unchecked-byte (Integer/parseInt (str x y) 16)))
                       (partition 2 s)))]
    (String. bytes "UTF-8")))

(defn -main
  [& args]
  (while true
    (let [resp (http/get "https://rainbows-and-unicorns.herokuapp.com/unicorns" {:as :text})
          bodyjson (json/read-str (-> @resp :body (clojure.string/replace #"\\r\\n\\r\\n" " ")))]
      (println (count bodyjson))
      (doall
        (map
          (fn [entry]
            (if (= (get entry "webhookEvent") "jira:issue_updated")
              (let [id (get-in entry ["issue" "id"])
                    status-from (get-in entry ["changelog" "items" 1 "fromString"])
                    status-to (get-in entry ["changelog" "items" 1 "toString"])]
                (println (clojure.string/join " " ["Updated:" id "from" status-from "to" status-to])))
              (println "Boop")))
          bodyjson)))
    (Thread/sleep 5000)
  )
  (let [service (getGmailService)
        user "me"
        message (->
          service
          (.users)
          (.messages)
          (.get user "152f63ee4b30e1c9")
          ;; (.setQ "from:bugzilla-daemon@meetup.com is:unread")
          ;;(.setMaxResults 1)
          (.setFormat "full")
          (.execute))]
    (println (->> message (.getPayload) (.getParts) (seq) (map (fn [x] (-> x (.getBody) (.decodeData) (String. "UTF-8"))))))))
