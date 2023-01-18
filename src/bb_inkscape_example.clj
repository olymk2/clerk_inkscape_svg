(ns bb-inkscape-example
  (:require [nextjournal.clerk :as clerk]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.data.xml :as xml]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

; # Example Inkscape babashka integration

;; I was exploring the idea of writing an inkscape plugin when I noticed shell was an option.
;; This notebook is an example on how to get your own script working
;; This gives quite a lot of power as we have a really nice xml story in the clojure world.
;; We can also interactively tets our script from our editor or even using a clerk notebook and rendering the output.

; ## INX file
;; To generate an inkscape plugin we need to create a configuration file, this is an xml document which ends in .inx

;; ^{::clerk/visibility {:code :hide}}
;; (clerk/html [:pre (slurp "src/bb_inkscape_example.inx")])

; ## Shell script

;; Along side the INX file we need a shell script to invoke bb, inkscape has support for a limited set of languages but shell being one we can send to babashka, would be nice to see native support here.
;; ^{::clerk/visibility {:code :hide}}
;; (clerk/html [:pre (slurp "src/bb_inkscape_example.sh")])

;; ## Functions for a simple Babashka script

(def cli-options
  [["-i" "--id ID" "Port number"]
   ["-h" "--help"]])

(defn node->str [node]
  (with-out-str (pprint node)))

(defn fetch-document->xml
  "Load an xml document and parse to an xml sequence
  set :namespace-aware to false so we don't get <svg:svg>"
  [document-file]
  (->>
   (xml/parse-str (slurp document-file) {:namespace-aware false})
   (xml-seq)))

(defn remove-node-by-id
  "filter the sequence looking for a specific element id attribute value"
  [node-id xml-tree]
  (filterv #(not= (-> % :attrs :id) node-id) xml-tree))

(defn fetch-node-by-id
  "filter the sequence looking for a specific element id attribute value"
  [node-id xml-tree]
  (filterv #(= (-> % :attrs :id) node-id) xml-tree))

(defn dump-fetched-node->file
  "Pass in the matched node and dump as xml to a tmp file."
  [element]
  (spit "/tmp/inkscape-selected-element.svg" (str element)))

;; # Clerk simple examples
;; These have been adjusted to use an svg in this repo, the filename and id inkscape give is specific to your running instance,
;; Also I needed the below hacky-svgify to render in clerk

(defn hacky-svgify
  "This is just a quick hacky function to render in clerk
  emit-str append <?xml doctype...... so do a basic regex to remove it
  wrap what ever is left in svg tags"
  [element]
  (str "<svg class=\"w-60 h-60\">" (str/replace-first
                element
                #"^.*?>" "") "</svg>"))


;; ## Load the xml document
(clerk/html
 (hacky-svgify
  (xml/emit-str
   (fetch-document->xml "src/clojure_logo.svg"))))

;; ## Find node by id and return only that node
;; Inkscape gives us the selected node so we can find it in this way
(clerk/html
 (->> (fetch-document->xml "src/clojure_logo.svg")
      (fetch-node-by-id "path12")
      (mapv #(xml/emit-str %))
      (first)
      (hacky-svgify)))

;; ## Filter out a node by id using postwalk
(clerk/html
 (let [xml-data (fetch-document->xml "src/clojure_logo.svg")]
   (->>  (clojure.walk/postwalk
          (fn [v]
            (cond
              ;; match id return nil to remove
              (= (-> v :attrs :id) "path18") nil
              ;; no match so return element as normal
              :else  v))
          xml-data)
         (xml/emit-str)
         (str/join "")
         (hacky-svgify))))

(clerk/show! "src/bb_inkscape_example.clj")

(comment (clerk/serve! {:browse? true}))

