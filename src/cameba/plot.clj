(ns cameba.plot
  (:require [cameba.svg2jfx :as svg2jfx]
            [thi.ng.geom.core :as g] ;; The graphing libraires
            [thi.ng.math.core :as m]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svgthing]))

(defn plot-spec
  "Given a size (WIDTH HEIGHT) the output map describes how the plot looks."
  [points output-width output-height]
  (let [width (count points)
        min (apply min points)
        max (apply max points)
        height (+ 2.2
                  (java.lang.Math/abs ^int max)
                  (java.lang.Math/abs ^int min))
        points-indexed (mapv #(vector %1 %2) (range) points)]
    {:x-axis (viz/linear-axis
              {:domain [0 width]
               :range  [0 output-width]
               ;; puts the axis out of view (can't show the grid with no axis)
               :pos    0
               :major 500})
     :y-axis (viz/linear-axis
              {:domain      [(- height) height]
               :range       [0 output-height]
               ;; puts the axis out of view (can't show the grid with no axis)
               :pos         0
               :label-dist  0
               :major 5000
               :label-style {:text-anchor "end"}})
     :grid   {:attribs {:stroke "#caa"}
              :minor-x false
              :minor-y false}
     :data   [{:values  points-indexed
               :attribs {:fill "none" :stroke "#f60" :stroke-width 2.25}
               :layout  viz/svg-line-plot}]}))

(defn plot-points
  ""
  [points width height]
;;  (println "Dimension -> width:" width "height:" height)
  (svg2jfx/svg-to-javafx-group (-> (plot-spec points width height)
                                   (viz/svg-plot2d-cartesian)
                                   (#(svgthing/svg {:width width
                                                    :height height}
                                                   %))
                                   (svgthing/serialize))))
