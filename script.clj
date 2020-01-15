(defn submit [f n]
  (dotimes [i 1] (mapv deref (map (fn [_] (future (f))) (range n)))))

;;Our original massive object allocating workload...
(defn work []
  (zipmap (range 500000) (range 500000)))

(let [xs (or (mapv clojure.edn/read-string *command-line-args*)
             [10 1])
      [n threads] xs]
  (time (dotimes [i (or n 1)]
          (submit work (or threads 1)))))

(System/exit 0)
