(ns cameba.core
  (:require [cameba.audio :as audio]
            [cameba.plot :as plot]
            [cljfx.api :as fx])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(defn- reset-sample-rate
  ""
  [state]
  (assoc state
         :current-sample-rate
         (first (keys (get-in state [:mixers
                                     (:current-mixer state)
                                     :lines
                                     (:current-line state)
                                     :formats
                                     (:current-channels state)
                                     (:current-bit-size state)
                                     (:current-endianness state)])))))


(defn- reset-endianness
  ""
  [state]
  (reset-sample-rate (assoc state
                            :current-endianness
                            (first (keys (get-in state [:mixers
                                                        (:current-mixer state)
                                                        :lines
                                                        (:current-line state)
                                                        :formats
                                                        (:current-channels state)
                                                        (:current-bit-size state)]))))))

(defn- reset-bit-size
  ""
  [state]
  (reset-endianness (assoc state
                           :current-bit-size
                           (first (keys (get-in state [:mixers
                                                       (:current-mixer state)
                                                       :lines
                                                       (:current-line state)
                                                       :formats
                                                       (:current-channels state)]))))))

(defn- reset-channels
  ""
  [state]
  (reset-bit-size (assoc state
                         :current-channels
                         (first (keys (get-in state [:mixers
                                                     (:current-mixer state)
                                                     :lines
                                                     (:current-line state)
                                                     :formats]))))))

(defn- reset-line  ""
  [state]
  (reset-channels (assoc state
                         :current-line
                         (first (keys (get-in state [:mixers
                                                     (:current-mixer state)
                                                     :lines]))))))
(defn- reset-mixer
  ""
  [state]
  (reset-line (assoc state
                     :current-mixer
                     (first (keys (:mixers state))))))





(def *state
  ""
  (atom (reset-mixer {:mixers (audio/load-audio-system)
                      :num-samples 1000})))


(defmulti event-handler
  "CLJFX -  Event Handlers

  When defining CLJFX event like `on-value-changed` you have two options
  - 1 You can point to *Function* (or lambda) to run when the event happens
  ex:
  ```
  {:fx/type :check-box
               :selected done
               :on-selected-changed #(swap! *state assoc-in [:by-id id :done] %)
  ```
  - 2 You can point to a custom *Map Events*
  ex:
  ```
  {:fx/type :check-box
               :selected done
               :on-selected-changed {:event/type ::set-done :id id}}
  ```
  You will then need to create an event handler function
  ex:
  ```
  (defn map-event-handler [event]
  (case (:event/type event)
    ::set-done (swap! *state assoc-in [:by-id (:id event) :done] (:fx/event event))))
  ```
  And this function will then be registered with the renderer
  ex:
  ```
  (fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))
  ```
  The map you paired to the event will be passed along to the registered event handler
  It will needs to designate its type with the `event/type` key
  And the actual even will be passed in under the `fx/event` key
  Any additional keys you place in the map will also get passed along (`:id` in the example)

  In the simple example we registered a function, however here I register a multimethod
  Then we simply switch on the `event/type`"
  :event/type)

(defmethod event-handler ::set-mixer
  [event]
  (swap! *state assoc :current-mixer (:fx/event event))
  (swap! *state reset-line))

(defmethod event-handler ::set-line
  [event]
  (swap! *state assoc :current-line (:fx/event event))
  (swap! *state reset-channels))

(defmethod event-handler ::set-channels
  [event]
  (swap! *state assoc :current-channels (:fx/event event))
  (swap! *state reset-bit-size))

(defmethod event-handler ::set-bit-size
  [event]
  (swap! *state assoc :current-bit-size (:fx/event event))
  (swap! *state reset-endianness))

(defmethod event-handler ::set-endianness
  [event]
  (swap! *state assoc :current-endianness (:fx/event event))
  (swap! *state reset-sample-rate))

(defmethod event-handler ::set-sample-rate
  [event]
  (swap! *state assoc :current-sample-rate (:fx/event event)))
;  (swap! *state reset-channels))


(defn get-current-line
  "Get the currently selected line object"
  [{:keys [mixers
           current-mixer
           current-line] :as state}]
  (get-in state [:mixers
                 current-mixer
                 :lines
                 current-line
                 :line]))

(defn get-current-format
  ""
  [{:keys [current-channels
           current-bit-size
           current-endianness
           current-sample-rate
           num-samples]
    :as state}]
  (audio/pcm-format current-channels
                    current-bit-size
                    (not current-endianness)
                    current-sample-rate))


(defn close-line
  "Closes the current line"
  [state]
  (audio/close-line (get-current-line state)))

(defn open-line
  "Opens the currently selected line with the current format
  And allocates a buffer for the result"
  [{:keys [current-channels
           current-bit-size
           current-endianness
           current-sample-rate
           num-samples] :as state}]
  (let [current-format (audio/pcm-format current-channels
                                         current-bit-size
                                         (not current-endianness)
                                         current-sample-rate)
        endianness  (if (not current-endianness)
                      java.nio.ByteOrder/BIG_ENDIAN
                      java.nio.ByteOrder/LITTLE_ENDIAN)]
    (println "state says endianness is: " current-endianness ". So setting line to:" endianness)
    (audio/open-line (get-current-line state)
                     current-format)
    (swap! *state
           assoc
           :byte-buffer
           (.order (java.nio.ByteBuffer/allocate (audio/calculate-buffer-size current-format num-samples))
                   endianness))))


(defn line-selection
  ""
  [{:keys [mixers
           current-mixer
           current-line
           current-channels
           current-bit-size
           current-endianness
           current-sample-rate]}]
  {:fx/type :h-box
   :children [{:fx/type :choice-box ;; DROPDOWN -> MIXER
               :on-value-changed {:event/type ::set-mixer}
               :value current-mixer
               :items (into [] (keys mixers))}
              {:fx/type :choice-box ;; DROPDOWN -> LINE
               :on-value-changed {:event/type ::set-line}
               :value current-line
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines])))}
              {:fx/type :choice-box ;; DROPDOWN -> CHANNELS
               :on-value-changed {:event/type ::set-channels}
               :value current-channels
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats])))}
              {:fx/type :choice-box ;; DROPDOWN -> BIT-SIZE
               :on-value-changed {:event/type ::set-bit-size}
               :value current-bit-size
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats
                                                     current-channels])))}
              {:fx/type :choice-box ;; DROPDOWN -> ENDIANNESS
               :on-value-changed {:event/type ::set-endianness}
               :value current-endianness
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats
                                                     current-channels
                                                     current-bit-size])))}
              {:fx/type :choice-box ;; DROPDOWN -> SAMPLE-RATE
               :on-value-changed {:event/type ::set-sample-rate}
               :value current-sample-rate
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats
                                                     current-channels
                                                     current-bit-size
                                                     current-endianness])))}]})

(defmethod event-handler ::read-into-buffer
  [event]
  (swap! *state assoc :current-mixer (:fx/event event)))


(defn plot-buffer
  ""
  [^java.nio.ByteBuffer
   buffer
   num-samples]
  (if (not (nil? buffer))
    (cameba.plot/plot-points (mapv #(vector %1 %2) (range) (audio/print-buffer buffer (* 2 num-samples)))
                             num-samples
                             (- (apply max (audio/print-buffer buffer num-samples)) (apply min (audio/print-buffer buffer num-samples))))))


(defn chart-view
  "Our plot"
  [{:keys [byte-buffer num-samples]}]
  (if (not (nil? byte-buffer))
    {:fx/type fx/ext-instance-factory
     :byte-buffer byte-buffer
     :num-samples num-samples
     :create #(plot-buffer byte-buffer num-samples)}
    {:fx/type :label
     :text "none"}))



(defn root
  "Takes the state atom (which is a map) and then get the mixers out of it and builds a windows with the mixers"
  [{:keys [mixers
           current-mixer
           current-line
           current-channels
           current-bit-size
           current-endianness
           current-sample-rate
           byte-buffer
           num-samples]
    :as state}]

  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type line-selection
                              :mixers mixers
                              :current-mixer current-mixer
                              :current-line current-line
                              :current-channels current-channels
                              :current-bit-size current-bit-size
                              :current-endianness current-endianness
                              :current-sample-rate current-sample-rate}]}}})
                             ;; {:fx/type :button
                             ;;  :text "Fire!"
                             ;;  :on-action #(do (print %) (audio/read-into-byte-buffer (get-current-line state)
                             ;;                                                         byte-buffer
                             ;;                                                         num-samples))}]}}})

                             ;; {:fx/type chart-view
                             ;;  :byte-buffer byte-buffer
                             ;;  :num-samples num-samples}]}}})



(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler event-handler}))

(fx/mount-renderer
 *state
 renderer)
