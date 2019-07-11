(ns cameba.audio)

;; The audio will be coming directly from Java's audio subsystem
;; I'm working off of the official guides:
;; https://docs.oracle.com/javase/tutorial/sound/capturing.html
;; (some sentences have been copied verbatim..)

;; ## Streaming
;; handeling audio one buffer/chunk at a time till the operations halts
;; you don;t know how long the data is

;; ## Unbuffered transport
;; Have all the data upfront and only supports playback
;; "unbuffered model allows sounds to be easily looped (cycled) or set
;;  to arbitrary positions in the data."


;; #  AudioSystem
;; This is the class we use to reading and writing file formats and converting between data formats
;; This will hide the format details for you and typically return a `AudioInputStream`
;; It will also get you all the components like *mixers*, *lines* and *ports*


;; To play or capture sound using the Java Sound API, you need at least three things:
;; *a line*
;; *a mixer*
;; *formatted audio data*

;; ## Line
;; A step in the audio pipeline. This can either be a mixer, a port or a *DataLine* between mixers/ports.
;; *DataLine* can be multichannel - this will be specified in the `AudioFormat` and will have additional
;; properties of the audio it's carrying (buffer size, level, media position etc.)
;; *TargetDataLine* receives audio data from a mixer
;; *SourceDataLine* receives audio data for playback
;; *Clip* is a data line into which audio data can be loaded prior to playback
;;
;; Later we will get our lines from the mixers themselves
;; Lines have Info objects that describe them

(set! *warn-on-reflection* true)

;; (defn parse-line-info
;;   "Take a LineInfo obejct and parse it's description into a map we can consume it later"
;;   [^javax.sound.sampled.Line$Info line-info]
;;   {:name (.toString line-info)
;;    :line-info line-info})

;; ;; For us we want to get audio from the mixer to display the waveform later
;; (defn get-target-lines
;;   "Gets the target/output lines of a mixer"
;;   [^javax.sound.sampled.Mixer mixer]
;;   (map parse-line-info (.getTargetLineInfo mixer)))


;; ;; ## Mixers
;; ;; Are a more generalized version of an audio "device"
;; ;; They take audio input and generate some audio output
;; ;; (input and output lines)
;; ;; Mixers encompass both physical and virtual devices
;; ;; They don't necessarily have to do any "mixing"
;; ;;
;; ;; We reference and look at Mixers through `MixerInfo` object
;; ;; Then with the `MixerInfo`s we can get the actual `Mixer` through the `AudioSystem`
;; (defn get-mixer
;;   "Given a `MixerInfo` returns the actual `Mixer`"
;;   ^javax.sound.sampled.Mixer [^javax.sound.sampled.Mixer$Info mixer-info]
;;   (javax.sound.sampled.AudioSystem/getMixer mixer-info))
;; ;;
;; ;; Each Mixer is pre-installed on the system.
;; ;; Its associated `MixerInfo` will have some properties
;; ;; And it will internally have input and output lines we can fetch
;; (defn parse-mixer-info
;;   "Read a mixer's properties from a `MixerInfo` into a map"
;;   [^javax.sound.sampled.Mixer$Info mixer-info]
;;   {:name (.getName mixer-info)
;;    :description (.getDescription mixer-info)
;;    :vendor (.getVendor mixer-info)
;;    :version (.getVersion mixer-info)
;;    :mixer-info mixer-info
;;    :target-lines-info (get-target-lines (get-mixer mixer-info))})

;; ;; The get the actual list of available mixers we can as the `AudioSystem`
;; ;; to get us a list of `MixerInfo` object.
;; (defn get-list-of-mixers
;;   "Gets all the mixers' properties from the AudioSystem and put them into a list"
;;   []
;;   (let [mixers (javax.sound.sampled.AudioSystem/getMixerInfo)]
;;     (into []
;;           (map-indexed (fn [idx itm] (assoc (parse-mixer-info itm) :index idx))
;;                        mixers))))

;; (defn get-current-mixer-info
;;   "Get the currently selected mixer"
;;   [state]
;;   (some #(if (= (:name %) (:current-mixer state)) %) (:mixers state)))

;; (defn get-current-line
;;   "Get the currently selected line"
;;   ^javax.sound.sampled.TargetDataLine [state]
;;   (let [mixer-info (get-current-mixer-info state)
;;         line-name (:current-line state)
;;         line-info (some #(if (= (:name %) line-name) %) (:target-lines-info mixer-info))]
;;     (.getLine (get-mixer (:mixer-info mixer-info)) (:line-info line-info))))

;; (defn get-current-line-info
;;   "Get the currently selected line"
;;   [state]
;;   (let [mixer-info (get-current-mixer-info state)
;;         line-name (:current-line state)
;;         line-info (some #(if (= (:name %) line-name) %) (:target-lines-info mixer-info))]
;;     (:line-info line-info)))

;;(get-current-line @cameba.core/*state)
;; => #object[com.sun.media.sound.PortMixer$PortMixerPort 0x254e5553 "com.sun.media.sound.PortMixer$PortMixerPort@254e5553"]




;; ## Ports
;; Actual connections between mixers an input/output from devices are represented by *ports*
;; So for instance the microphone input will be a port on the sound card mixer

;; The distinction between Lines and Ports is a bit blurry, but we don't/can't read directly from Ports



;; ## Formatted Audio Data
;; ### Dataformat
;; *the data representation of the sound*
;; a data format is represented by an `AudioFormat` object
;;
;; - Encoding technique, usually pulse code modulation (PCM)
;; - Number of channels (1 for mono, 2 for stereo, etc.)
;; - Sample rate (number of samples per second, per channel)
;; - Number of bits per sample (per channel)
;; - Frame rate
;; - Frame size in bytes
;; - Byte order (big-endian or little-endian)
;;
;; A line will typically have some default format

;; # Reading a Line
;; At this point we want to open and read some data in to play around with
;;
;; We already know how to get a Line from the Mixer. We want to get a Line that is a "TargetDataLine"
;; Then we can open it and read in some data



;; (defn open-line
;;   "initializes and opens the LINE"
;;   [line]
;;   (.open ^javax.sound.sampled.TargetDataLine line
;;          (.getFormat line)))



;; (defn read-into-byte-buffer
;;   "Reads LINE input into BYTE-BUFFER"
;;   [line byte-buffer num-samples]
;;   (.start ^javax.sound.sampled.TargetDataLine line)
;;   (.read ^javax.sound.sampled.TargetDataLine line (.array byte-buffer) 0 num-samples)
;;   (.stop ^javax.sound.sampled.TargetDataLine line))


;; (audio/read-into-byte-buffer  (get-current-line @*state) (:byte-buffer @*state) (:num-samples @*state))






;; (def my-format (.getFormat my-line))


;; (in-ns 'cameba.core)
;; (def my-line (get-current-line @*state))
;; (def my-format (.getFormat my-line))

;; (.isActive my-line)
;; (.isRunning my-line)
;; (.isOpen my-line)
;;
;; (def my-size-buffer (.getBufferSize my-line))
;; (def my-size-buffer 1000)

;; (.open ^javax.sound.sampled.TargetDataLine my-line my-format)
;; (.isOpen my-line)

;; (def my-buffer (byte-array my-size-buffer))

;; (.start ^javax.sound.sampled.TargetDataLine my-line)

;; (.read ^javax.sound.sampled.TargetDataLine my-line my-buffer 0 my-size-buffer)


;; (def my-bb (java.nio.ByteBuffer/allocate my-size-buffer))
;; (.put my-bb my-buffer)




;; ### Fileformat
;; represented by an AudioFileFormat object

;; - The file type (WAVE, AIFF, etc.)
;; - The file's length in bytes
;; - The length, in frames, of the audio data contained in the file
;; - An AudioFormat object that specifies the data format of the audio data contained in the file)


(defn- pcm?
  "Check if a format is PCM ( pulse code modulation ) - ie. straigh uncompressed bytes"
  [^javax.sound.sampled.AudioFormat
   format]
  (let [encoding (.getEncoding format)]
    (or (= encoding javax.sound.sampled.AudioFormat$Encoding/PCM_SIGNED)
        ;;(= encoding javax.sound.sampled.AudioFormat.Encoding/PCM_FLOAT))))
        ;; TODO: Maybe support this?
        (= encoding javax.sound.sampled.AudioFormat$Encoding/PCM_UNSIGNED))))

(defn- one-channel?
  "Check if there is more than one channel"
  [^javax.sound.sampled.AudioFormat
   format]
  (= 1 (.getChannels format)))

(defn- parse-formats
  "Reads in the compatible formats into a tree of possibilites
  Keys decend in the following order:
  Channels ->
  Sample Size in Bits ->
  Big Endianness (true/false)

  You can then use
  ```
  (get-in formats [channels sample-size-in-bits big-endian?])
  ```
  if returns `true` then the formats is available for the line "
  [^javax.sound.sampled.AudioFormat
   formats]
  ;; builds a tree of possible formats
  (reduce #(assoc-in %1 [(.getChannels ^javax.sound.sampled.AudioFormat %2)
                         (.getSampleSizeInBits ^javax.sound.sampled.AudioFormat %2)
                         (.isBigEndian ^javax.sound.sampled.AudioFormat %2)
                         44100]
                     nil);(.toString ^javax.sound.sampled.AudioFormat %2))
          {}
          ;; we only support PCM formats for now and only on a single channel
          (filter one-channel? (filter pcm? formats))))


(defn- parse-line
  "Given a mixer and line-info it will make a map
  ```
  {^String line-name {:line ^Line line
                      :formats { ... see (parse-formats) .. }}
  }
  ```
  "
  [^javax.sound.sampled.Mixer mixer
   ^javax.sound.sampled.DataLine$Info line-info]
  (if (= javax.sound.sampled.TargetDataLine (.getLineClass line-info))
    {(.toString line-info) {:line (.getLine mixer line-info)
                            :formats (parse-formats (.getFormats line-info))}}
    {}))

(defn- parse-all-lines
  "Given a mixer and its associate line-infos, parse them into a map"
  [^javax.sound.sampled.Mixer mixer
   ^javax.sound.sampled.DataLine$Info line-infos]
  ;; TODO: Is there a way to write this without the ugly `into {}` ?
  (into {} (map (partial parse-line mixer) line-infos)))

(defn- parse-mixer
  "Take a mixer-info and make a map of the mixer and it's associated lines"
  [^javax.sound.sampled.Mixer$Info mixer-info]
  (let [mixer ^javax.sound.sampled.Mixer (javax.sound.sampled.AudioSystem/getMixer mixer-info)
        target-line-infos (.getTargetLineInfo mixer)
        target-lines (parse-all-lines mixer target-line-infos)]
    (if (empty? target-lines)
      {} ;; if the mixer has no target data lines then discard
      {(.toString mixer-info) {:mixer mixer
                               :lines target-lines}})))

(defn- parse-all-mixers
  "Given a list of mixer-infos, return a map of {^String MixerName {:mixer mixer .... } pairs"
  [mixer-infos]
  (into {} (map parse-mixer mixer-infos)))


(defn load-audio-system
  "Reads in all Mixers/Lines/Formats into a map"
  []
  (let [mixer-infos (javax.sound.sampled.AudioSystem/getMixerInfo)]
     (parse-all-mixers mixer-infos)))


(defn pcm-format
  "Builds a linear PCM AudioFormat object based on parameters"
  [channels bit-size endianness sample-rate]
  
  (javax.sound.sampled.AudioFormat. sample-rate
                                    bit-size
                                    channels
                                    (not= bit-size 8) ;; TODO: a bit of a hack..
                                    endianness))

(defn open-line
  ""
  [^javax.sound.sampled.TargetDataLine
   line
   ^javax.sound.sampled.AudioFormat
   format]
  (println "Is line open? "(.isOpen ^javax.sound.sampled.TargetDataLine line))
  (println "Opening line")
  (.open line format)
  (println "Is line open? "(.isOpen line)))


(defn close-line
  ""
  [^javax.sound.sampled.TargetDataLine
   line]
  (println "Closing line")
  (if (.isOpen line) (.close line))
  (println "Is line open? "(.isOpen line)))


(defn read-into-byte-buffer
  "Reads LINE input into BYTE-BUFFER"
  [line
   ^java.nio.ByteBuffer
   buffer
   num-samples]
  (.start ^javax.sound.sampled.TargetDataLine line)
  (println "Reading in this many samples: "(.read ^javax.sound.sampled.TargetDataLine line
                                                  (.array buffer)
                                                  0
                                                  (* 2 num-samples)))
  (.stop ^javax.sound.sampled.TargetDataLine line))


(defn calculate-buffer-size
  "Make a ByteBuffer for a given FORMAT and a given number of samples NUM-SAMPLES"
  [^javax.sound.sampled.AudioFormat
   format
   num-samples]
  (let [channels (.getChannels format)
        frame-rate (.getFrameRate format)
        sample-size (.getSampleSizeInBits format)]
    (/ (* channels sample-size num-samples) 8)))



(defn print-buffer-in-binary
  ""
  [^java.nio.ByteBuffer
   buffer
   num-samples]
  (map #(Integer/toBinaryString (.getShort buffer %)) (range 0 num-samples 1)))


(defn print-buffer
  ""
  [^java.nio.ByteBuffer
   buffer
   num-samples]
  (map #(.getShort buffer %) (range 0 (- num-samples 1) 2)))
;; (audio/print-buffer (:byte-buffer @*state) 1000)

