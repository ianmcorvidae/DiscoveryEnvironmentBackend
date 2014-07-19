(ns irods-avu-migrator.core
  (:gen-class)
  (:use [irods-avu-migrator.ipc-units]
        [irods-avu-migrator.templates])
  (:require [irods-avu-migrator.db :as db]
            [common-cli.version :as version]
            [common-cli.core :as ccli]
            [clojure.tools.cli :as cli]))

(def commands
  #{"import-template-avus"
    "remove-template-avus"
    "delete-ipc-user-units"
    "--version"
    "--help"})

(def base-options
  [["-v" "--version"]
   ["-h" "--help"]])

(def db-options
  [["-d" "--db-host HOST" "The hostname for the DE database"]

   ["-b" "--db-port PORT" "The port for the DE database"
    :default "5432"]

   ["-u" "--db-user USER" "The username for the DE datbase"
    :default "de"]

   ["-n" "--db-name DB" "The name of the DE database"
    :default "de"]

   ["-m" "--db-metadata-name DB" "The name of the Metadata database"
    :default "metadata"]])

(def icat-options
  [["-i" "--icat-host HOST" "The hostname for the ICAT database"]

   ["-c" "--icat-port PORT" "The port for the ICAT database"
    :default "5432"]

   ["-a" "--icat-user USER" "The username for the ICAT database"]

   ["-t" "--icat-name DB" "The name of the ICAT database"
    :default "ICAT"]])

(def options
  (concat
   base-options
   db-options
   icat-options))

(def app-info
  {:desc "DE tool for migrating AVU metadata from iRODS to PostgreSQL"
   :app-name "irods-avu-migrator"
   :group-id "org.iplantc"
   :art-id "irods-avu-migrator"})

(defn- command
  [cmd options]
  (case cmd
    "import-template-avus" (import-template-avus options)
    "remove-template-avus" (remove-template-avus options)
    "delete-ipc-user-units" (convert-ipc-units options)))

(defn- commands-help
  []
  (println "Command must be one of:" commands)
  (println "Each command has its own --help."))

(defn -main
  [& args]
  (when-not (contains? commands (first args))
    (commands-help)
    (System/exit 1))

  (let [cmd      (first args)
        cmd-args (rest args)
        {:keys [desc app-name group-id art-id]}    app-info
        {:keys [options arguments errors summary]} (cli/parse-opts cmd-args options)]
    (cond
     (= cmd "--help")
     (do
       (commands-help)
       (System/exit 0))

     (or (= cmd "--version") (:version options))
     (ccli/exit 0 (version/version-info group-id art-id))

     (:help options)
     (ccli/exit 0 (ccli/usage desc app-name summary))

     errors
     (ccli/exit 1 (ccli/error-msg errors))

     (not (:icat-host options))
     (ccli/exit 1 "You must specify an --icat-host.")

     (not (:icat-user options))
     (ccli/exit 1 "You must specify an --icat-user"))

    (command cmd options)))
