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

(defn parse-line-info
  "Take a LineInfo obejct and parse it's description into a map we can consume it later"
  [^javax.sound.sampled.Line$Info line-info]
  {:name (.toString line-info)
   :line-info line-info})

;; For us we want to get audio from the mixer to display the waveform later
(defn get-target-lines
  "Gets the target/output lines of a mixer"
  [^javax.sound.sampled.Mixer mixer]
  (map parse-line-info (.getTargetLineInfo mixer)))


;; ## Mixers
;; Are a more generalized version of an audio "device"
;; They take audio input and generate some audio output
;; (input and output lines)
;; Mixers encompass both physical and virtual devices
;; They don't necessarily have to do any "mixing"
;;
;; We reference and look at Mixers through `MixerInfo` object
;; Then with the `MixerInfo`s we can get the actual `Mixer` through the `AudioSystem`
(defn get-mixer
  "Given a `MixerInfo` returns the actual `Mixer`"
  ^javax.sound.sampled.Mixer [^javax.sound.sampled.Mixer$Info mixer-info]
  (javax.sound.sampled.AudioSystem/getMixer mixer-info))
;;
;; Each Mixer is pre-installed on the system.
;; Its associated `MixerInfo` will have some properties
;; And it will internally have input and output lines we can fetch
(defn parse-mixer-info
  "Read a mixer's properties from a `MixerInfo` into a map"
  [^javax.sound.sampled.Mixer$Info mixer-info]
  {:name (.getName mixer-info)
   :description (.getDescription mixer-info)
   :vendor (.getVendor mixer-info)
   :version (.getVersion mixer-info)
   :mixer-info mixer-info
   :target-lines-info (get-target-lines (get-mixer mixer-info))})

;; The get the actual list of available mixers we can as the `AudioSystem`
;; to get us a list of `MixerInfo` object.
(defn get-list-of-mixers
  "Gets all the mixers' properties from the AudioSystem and put them into a list"
  []
  (let [mixers (javax.sound.sampled.AudioSystem/getMixerInfo)]
    (into []
          (map-indexed (fn [idx itm] (assoc (parse-mixer-info itm) :index idx))
                       mixers))))

(defn get-current-mixer-info
  "Get the currently selected mixer"
  [state]
  (some #(if (= (:name %) (:current-mixer state)) %) (:mixers state)))

(defn get-current-line
  "Get the currently selected line"
  [state]
  (let [mixer-info (get-current-mixer-info state)
        line-name (:current-line state)
        ^javax.sound.sampled.Line$Info line-info (some #(if (= (:name %) line-name) %) (:target-lines-info mixer-info))]
    (.getLine (get-mixer (:mixer-info mixer-info)) (:line-info line-info))))

(defn get-current-line-info
  "Get the currently selected line"
  [state]
  (let [mixer-info (get-current-mixer-info state)
        line-name (:current-line state)
        line-info (some #(if (= (:name %) line-name) %) (:target-lines-info mixer-info))]
    (:line-info line-info)))

;;(get-current-line @cameba.core/*state)
;; => #object[com.sun.media.sound.PortMixer$PortMixerPort 0x254e5553 "com.sun.media.sound.PortMixer$PortMixerPort@254e5553"]




;; ## Ports
;; Actual connections between mixers an input/output from devices are represented by *ports*
;; So for instance the microphone input will be a port on the sound card mixer





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
;; ### Fileformat
;; represented by an AudioFileFormat object

;; - The file type (WAVE, AIFF, etc.)
;; - The file's length in bytes
;; - The length, in frames, of the audio data contained in the file
;; - An AudioFormat object that specifies the data format of the audio data contained in the file)
