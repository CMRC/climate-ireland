(ns clad.pw
  (:require (cemerick.friend [credentials :as creds])))
  
(def users {"guest" {:username "guest"
                     :password (creds/hash-bcrypt "climate")
                     :roles #{::user}}})

