(ns metadactyl.routes.apps
  (:use [metadactyl.app-listings :only [get-all-app-ids
                                        get-app-details
                                        get-app-description
                                        search-apps]]
        [metadactyl.app-validation :only [app-publishable?]]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.params]
        [metadactyl.zoidberg :only [edit-app copy-app edit-workflow]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.service.app-metadata :as app-metadata]
            [metadactyl.util.service :as service]
            [compojure.route :as route]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* apps
  (GET* "/" []
        :query [params AppSearchParams]
        :summary "Search Apps"
        :return AppListing
        :notes "This service allows users to search for Apps based on a part of the App name or
        description. The response body contains an `apps` array that is in the same format as
        the `apps` array in the /apps/categories/:category-id endpoint response."
        (service/trap #(search-apps params)))

  (GET* "/:app-id/details" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :return AppDetails
        :summary "Get App Details"
        :notes "This service is used by the DE to obtain high-level details about a single App"
        (service/trap #(get-app-details app-id)))

  (GET* "/:app-id/ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :return App
        :summary "Make an App Available for Editing"
        :notes "The app integration utility in the DE uses this service to obtain the App
        description JSON so that it can be edited. The App must have been integrated by the
        requesting user, and it must not already be public."
        (service/trap #(edit-app app-id)))

  (POST* "/arg-preview" []
         :query [params SecuredQueryParams]
         :body [body (describe App "The App to preview.")]
         :summary "Preview Command Line Arguments"
         :notes "The app integration utility in the DE uses this service to obtain an example list
         of command-line arguments so that the user can tell what the command-line might look like
         without having to run a job using the app that is being integrated first. The App request
         body also requires that each parameter contain a `value` field that contains the parameter
         value to include on the command line. The response body is in the same format as the
         `/arg-preview` service in the JEX. Please see the JEX documentation for more information."
         (ce/trap "arg-preview" #(service/swagger-response (app-metadata/preview-command-line body))))

  (PATCH* "/:app-id" []
          :path-params [app-id :- AppIdPathParam]
          :query [params SecuredQueryParams]
          :body [body (describe App "The App to update.")]
          :summary "Update App Labels"
          :notes "This service is capable of updating just the labels within a single-step app, and
          it allows apps that have already been made available for public use to be updated, which
          helps to eliminate administrative thrash for app updates that only correct typographical
          errors. Upon success, the response body contains just a success flag. Upon error, the
          response body contains an error code along with some additional information about the
          error. <b>Note</b>: Although this endpoint accepts all App fields, only the 'name' (except
          in parameters and parameter arguments), 'description', 'label', and 'display' (only in
          parameter arguments) fields will be processed and updated by this endpoint."
          (ce/trap "update-app-labels" #(app-metadata/relabel-app (assoc body :id app-id))))

  (POST* "/:app-id/copy" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :summary "Make a Copy of an App Available for Editing"
         :notes "This service can be used to make a copy of an App in the user's workspace."
         (service/trap #(copy-app app-id)))

  (GET* "/:app-id/pipeline-ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :return Pipeline
        :summary "Make a Pipeline Available for Editing"
        :notes "The DE uses this service to obtain a JSON representation of a Pipeline for editing.
        The Pipeline must have been integrated by the requesting user, and it must not already be
        public."
        (service/trap #(edit-workflow app-id)))

  (POST* "/:app-id/copy-pipeline" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :summary "Make a Copy of a Pipeline Available for Editing"
         :notes "This service can be used to make a copy of a Pipeline in the user's workspace."
         (service/trap #(copy-app app-id)))

  (GET* "/:app-id/is-publishable" [app-id]
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Determine if an App Can be Made Public"
        :notes "A multi-step App can't be made public if any of the Tasks that are included in it
        are not public. This endpoint returns a true flag if the App is a single-step App or it's a
        multistep App in which all of the Tasks included in the pipeline are public."
        (ce/trap "is-publishable" #(hash-map :publishable (first (app-publishable? app-id)))))

  (DELETE* "/:app-id" []
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Logically Deleting an App"
           :notes "An app can be marked as deleted in the DE without being completely removed from
           the database using this service. <b>Note</b>: an attempt to delete an App that is already
           marked as deleted is treated as a no-op rather than an error condition. If the App
           doesn't exist in the database at all, however, then that is treated as an error condition."
           (ce/trap "delete-app" #(app-metadata/delete-app app-id)))

  (POST* "/shredder" []
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Logically Deleting Apps"
         :notes "One or more Apps can be marked as deleted in the DE without being completely
         removed from the database using this service. <b>Note</b>: an attempt to delete an app that
         is already marked as deleted is treated as a no-op rather than an error condition. If the
         App doesn't exist in the database at all, however, then that is treated as an error
         condition."
         (ce/trap "apps-shredder" #(app-metadata/delete-apps body)))

  (GET* "/:app-id/description" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Get an App Description"
        :notes "This service is used by Donkey to get App descriptions for job status update
        notifications. There is no request body and the response body contains only the App
        description, with no special formatting."
        (service/trap #(get-app-description app-id)))

  (GET* "/ids" []
        :query [params SecuredQueryParams]
        :return AppIdList
        :summary "List All App Identifiers"
        :notes "The export script needs to have a way to obtain the identifiers of all of the apps
        in the Discovery Environment, deleted or not. This service provides that information."
        (service/trap #(get-all-app-ids))))