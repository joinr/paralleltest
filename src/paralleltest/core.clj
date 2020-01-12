(ns paralleltest.core
  (:require [clojure.core.async :as async]
            [primitive-math :as p]))

(.availableProcessors (Runtime/getRuntime))
;;8

(defn submit [f n]
  (time (dotimes [i 1] (mapv deref (map (fn [_] (future (f))) (range n))))))

;;Our original massive object allocating workload...
(defn work []
  (zipmap (range 500000) (range 500000)))

;;Lighter allocation workloads...
(defn add-empty []
  (dotimes [i 10000000] (assoc {} :a 2)))

(defn add-map []
  (dotimes [i 10000000] (assoc {:a 2 :b 3} :a 2)))

(submit work 1)
;;"Elapsed time: 213.985636 msecs"
(submit work 8)
;;"Elapsed time: 740.427077 msecs"

;;Lol, wtf, why is this just like 3.73x faster? makes no sense!
(submit add-map 1)
;;"Elapsed time: 56.790034 msecs"

(submit add-map 8)
;;"Elapsed time: 123.411164 msecs"

(submit add-empty 1)
;;"Elapsed time: 199.970449 msecs"
(submit add-empty 8)
;;"Elapsed time: 390.509209 msecs"

(defn numeric-work []
  (loop [i 0 acc 0]
    (if (p/< i 500000000)
      (recur (p/inc i) (p/+ 1 1))
      acc)))

(defn numeric-work-obj []
  (dotimes [i 500000000]
    (unchecked-add 1 1)))

;;I figured we'd have our closes opportunity for
;;a proportional speedup, but n!  Very strange...
;;since we shouldn't a allocating anything!
(submit numeric-work 1)
"Elapsed time: 156.91116 msecs"
(submit numeric-work 8)
"Elapsed time: 343.235013 msecs"

(comment
  (/ 735.86 210.06)
  ;;3.503094353994097

  ;;alternate implementation using core.async threads.
  (defn pmap!
    ([n f xs]
     (let [output-chan (async/chan)]
       (async/pipeline-blocking n
                                output-chan
                                (map f)
                                (async/to-chan xs))
       (async/<!! (async/into [] output-chan))))
    ([f xs] (pmap! (.availableProcessors (Runtime/getRuntime)) f  xs)))

  (time (dotimes [i 1] (pmap! 8 (fn [_] (work)) (range 8))))
  ;;"Elapsed time: 768.104268 msecs"

  ;;an unordered attempt.
  (defn work-n [n f]
    (let [result   (promise)
          xs       (atom [])
          _        (add-watch xs :done
                              (fn [_ _ old new]
                                (when (== (count new) n)
                                  (deliver result new))))
          submit   (fn [x]
                     (swap! xs conj x))
          workers (dotimes [i n]
                    (future (submit (f))))]
      @result))

  (time (dotimes [i 1] (work-n 1 work)))
  ;;"Elapsed time: 213.370527 msecs"

  (time (dotimes [i 1] (work-n 8 work)))
  ;;"Elapsed time: 798.423232 msecs"

)
