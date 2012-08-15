(ns clad.pw
  (:require (cemerick.friend [credentials :as creds]))
  (:use [clad.models.couch]))
  
(def users (get-users))


