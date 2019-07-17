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
         (first (keys (get-in
                       state
                       [:mixers
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
  (reset-sample-rate (assoc
                      state
                      :current-endianness
                      (first (keys (get-in
                                    state
                                    [:mixers
                                     (:current-mixer state)
                                     :lines
                                     (:current-line state)
                                     :formats
                                     (:current-channels state)
                                     (:current-bit-size state)]))))))

(defn- reset-bit-size
  ""
  [state]
  (reset-endianness (assoc
                     state
                     :current-bit-size
                     (first (keys (get-in
                                   state
                                   [:mixers
                                    (:current-mixer state)
                                    :lines
                                    (:current-line state)
                                    :formats
                                    (:current-channels state)]))))))

(defn- reset-channels
  ""
  [state]
  (reset-bit-size (assoc
                   state
                   :current-channels
                   (first (keys (get-in
                                 state
                                 [:mixers
                                  (:current-mixer state)
                                  :lines
                                  (:current-line state)
                                  :formats]))))))

(defn- reset-line
  ""
  [state]
  (reset-channels (assoc
                   state
                   :current-line
                   (first (keys (get-in
                                 state
                                 [:mixers
                                  (:current-mixer state)
                                  :lines]))))))
(defn- reset-mixer
  ""
  [state]
  (reset-line (assoc
               state
               :current-mixer
               (first (keys (:mixers state))))))



(defn get-current-line
  "Get the currently selected line object"
  [{:keys [mixers
           current-mixer
           current-line]}]
  (get-in mixers
          [current-mixer
           :lines
           current-line
           :line]))

(defn get-current-format
  ""
  [{:keys [current-channels
           current-bit-size
           current-endianness
           current-sample-rate
           num-samples]}]
  (audio/pcm-format current-channels
                    current-bit-size
                    (not current-endianness)
                    current-sample-rate))


(defn clear-buffer
  "Closes the current line and returns a nil for the new buffer"
  [state]
  (audio/close-line (get-current-line state))
  (assoc state
         :byte-buffer
         nil))

(defn initialize-buffer
  "1 - Opens the current line
  2 - Initializes the byte buffer (for the current line and format)"
  [state]
  (let [line (get-current-line state)
        format (get-current-format state)]
    (audio/open-line line format)
    (assoc state
           :byte-buffer
           (audio/allocate-byte-buffer format (:num-samples state)))))

(def *state
  ""
  (atom (reset-mixer
         {:mixers (audio/load-audio-system)
          :num-samples 1000
          :width 1000
          :height 500})))


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
    ::set-done (swap! *state assoc-in
                             [:by-id (:id event) :done]
                             (:fx/event event))))
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
  The map you paired to the event will be passed to the registered event handler
  It will needs to designate its type with the `event/type` key
  And the actual even will be passed in under the `fx/event` key
  Any additional keys you place in the map will also get passed along
  lik `:id` in the example

  In the simple example we registered a function,
  however here I register a multimethod
  Then we simply switch on the `event/type`"
  :event/type)

(defmethod event-handler ::set-mixer
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-mixer (:fx/event event))
  (swap! *state reset-line)
  (swap! *state initialize-buffer))

(defmethod event-handler ::set-line
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-line (:fx/event event))
  (swap! *state reset-channels)
  (swap! *state initialize-buffer))

(defmethod event-handler ::set-channels
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-channels (:fx/event event))
  (swap! *state reset-bit-size)
  (swap! *state initialize-buffer))

(defmethod event-handler ::set-bit-size
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-bit-size (:fx/event event))
  (swap! *state reset-endianness)
  (swap! *state initialize-buffer))

(defmethod event-handler ::set-endianness
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-endianness (:fx/event event))
  (swap! *state reset-sample-rate)
  (swap! *state initialize-buffer))

(defmethod event-handler ::set-sample-rate
  [event]
  (swap! *state clear-buffer)
  (swap! *state assoc :current-sample-rate (:fx/event event))
  (swap! *state initialize-buffer))

;; (swap! *state
;;        assoc
;;        :byte-buffer
;;        (allocate-buffer



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
               :fx/key [:mix current-mixer]
               :on-value-changed {:event/type ::set-mixer}
               :value current-mixer
               :items (into [] (keys mixers))}
              {:fx/type :choice-box ;; DROPDOWN -> LINE
               :fx/key [:line current-mixer current-line]
               :on-value-changed {:event/type ::set-line}
               :value current-line
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines])))}
              {:fx/type :choice-box ;; DROPDOWN -> CHANNELS
               :fx/key [:line current-mixer current-line current-channels]
               :on-value-changed {:event/type ::set-channels}
               :value current-channels
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats])))}
              {:fx/type :choice-box ;; DROPDOWN -> BIT-SIZE
               :fx/key [:line current-mixer current-line current-channels current-bit-size]
               :on-value-changed {:event/type ::set-bit-size}
               :value current-bit-size
               :items (into [] (keys (get-in mixers [current-mixer
                                                     :lines
                                                     current-line
                                                     :formats
                                                     current-channels])))}
              {:fx/type :choice-box ;; DROPDOWN -> ENDIANNESS
               :fx/key [:line current-mixer current-line current-channels current-bit-size current-endianness]
               :on-value-changed {:event/type ::set-endianness}
               :value current-endianness
               :items (into [] (keys (get-in
                                      mixers
                                      [current-mixer
                                       :lines
                                       current-line
                                       :formats
                                       current-channels
                                       current-bit-size])))}
              {:fx/type :choice-box ;; DROPDOWN -> SAMPLE-RATE
               :fx/key [:line current-mixer current-line current-channels current-bit-size current-endianness current-sample-rate]
               :on-value-changed {:event/type ::set-sample-rate}
               :value current-sample-rate
               :items (into [] (keys (get-in
                                      mixers
                                      [current-mixer
                                       :lines
                                       current-line
                                       :formats
                                       current-channels
                                       current-bit-size
                                       current-endianness])))}]})

(defmethod event-handler ::read-into-buffer
  [event]
  (audio/read-into-byte-buffer (get-current-line @*state)
                               (:byte-buffer @*state)
                               (:num-samples @*state)
                               (:current-bit-size @*state)))

(defn plot-buffer
  ""
  [^java.nio.ByteBuffer
   buffer
   num-samples
   bit-size
   width
   height]
  (if (not (nil? buffer))
    (plot/plot-points (audio/read-out-byte-buffer buffer
                                                  num-samples
                                                  bit-size)
                      width
                      height)))

(defn chart-view
  "Our plot"
  [{:keys [byte-buffer
           num-samples
           bit-size
           width
           height]}]
  (if (not (nil? byte-buffer))
    {:fx/type fx/ext-instance-factory
     :byte-buffer byte-buffer
     :num-samples num-samples
     :create #(plot-buffer byte-buffer
                           num-samples
                           bit-size
                           width
                           (- height 100))}
    {:fx/type :label
     :text "none"}))

(defmethod event-handler ::width-changed
  [event]
  (swap! *state assoc :width (:fx/event event)))

(defmethod event-handler ::height-changed
  [event]
  (swap! *state assoc :height (:fx/event event)))

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
           num-samples
           width
           height]
    :as state}]

  {:fx/type :stage
   :showing true
   :width width
   :height height
   :on-width-changed {:event/type ::width-changed}
   :on-height-changed {:event/type ::height-changed}
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type line-selection
                              :mixers mixers
                              :current-mixer current-mixer
                              :current-line current-line
                              :current-channels current-channels
                              :current-bit-size current-bit-size
                              :current-endianness current-endianness
                              :current-sample-rate current-sample-rate}
                             {:fx/type :button
                              :text "Fire!"
                              :on-action {:event/type ::read-into-buffer}}

                             {:fx/type chart-view
                              :byte-buffer byte-buffer
                              :num-samples num-samples
                              :bit-size current-bit-size
                              :width width
                              :height height}]}}})



(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler event-handler}))

(fx/mount-renderer
 *state
 renderer)


;; TODOS
;;
;; - 20 bit sound
;; - non-PCM formats
;;
