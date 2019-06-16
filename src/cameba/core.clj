(ns cameba.core
  (:require [cljfx.api :as fx]
            [cameba.audio :as audio])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(def *state
  "A map that describes the GUI state (in a generic, non-JFX specific way)"
  (let [mixers (audio/get-list-of-mixers)
        first-mixer (first mixers)]
   (atom {:current-mixer (:name first-mixer) :mixers mixers})))


(defn choice-box
  "Make a drop down choice-box of the audio devices"
  [mixers]
 (map :name mixers))
;;  (map #(str (:index %) ": " (:name %)) mixers))

;; This is the tree of windows and toggles and whatnot. It takes in the a current state variable
(defn root
  "Takes the state atom (which is a map) and then get the mixers out of it and builds a windows with the mixers"
  [{:keys [mixers current-mixer]}]

  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :children [{:fx/type :choice-box
                                     :pref-width 200
                                     :pref-height 40
                                     :on-value-changed #(swap! *state assoc :current-mixer %)
                                     :value current-mixer
                                     :items (choice-box mixers)}
                             {:fx/type :choice-box
                                     :pref-width 200
                                     :pref-height 40
                                     :on-value-changed #(swap! *state assoc :current-line %)
                                     :value current-mixer
                                     :items (choice-box (:target-lines-info (some #(if (= (:name %) (:current-mixer @*state)) %) (:mixers @*state))))}]}}})


(fx/mount-renderer
  *state
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)))
