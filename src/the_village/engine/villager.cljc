(ns the-village.engine.villager
  (:require [the-village.engine.journey :as journey]
            [the-village.utils.logging :as logging]))

;; a Villager lives in the village. It's a pretty stupid
;; animal that needs to fulfill vital needs : drink, eat, sleep.
;; You never tell a villager what to do. You suggest
;; through its environment.

(defprotocol Vital
  (update-vitals [this conf] "update vitals attributes"))

(defprotocol FreeWill
  (update-will [this] "update what it wants to do"))


(defn willing-to?
  "Does this `villager` is willing to `will` ?"
  [{:keys [wills] :as _villager} will]
  (some #{will} wills))

(defn thirsty?
  "Does this `villager` is `thirsty` ?"
  [{:keys [hydration-level] :as _villager}]
  (and (not (willing-to? _villager :villager.will/drink))
       (< hydration-level 0.99)))

(defn willing-to
  "Prepend `will` to list of wills of `villager`"
  [villager will]
  (update villager :wills (partial cons will)))

(defn on-journey?
  "Does this villager on journey ?"
  [{:keys [journey] :as _villager}]
  (some? journey))

(defn new-journey
  "This villager abort current journey (if there is one)
  and try to fulfill the one provided by mk-journey"
  [villager mk-journey]
  (assoc villager :journey (mk-journey villager)))

(defrecord Villager
  [;; Every villager are registered at an office once
   ;; they set foot in the village. Here a pleasant bureaucrat
   ;; will give him/her a number that identifies him/her
   ;; uniquely among the other. They are just numbers after
   ;; all !
   id

   ;; Hydration level : is an float ranging from 0 to 1
   ;; when it reaches 0 this villager should die
   ;; (unless he is not human, so why would he be thirsty ?)
   ;; when it's 1, this human has quenched his thirst.
   ;; If a villager die of dehydration in 48h
   ;; that means he needs to walk regularly to the well,
   ;; and bring back a full bucket of water to his house
   ;; (this last part is not part of the simulation).
   hydration-level

   ;; To do list for this villager. When the list is empty,
   ;; the villager is idle. His vital perks keeps being
   ;; updated. The list is ordered by priority. The first
   ;; will have to be completed with the highest priority.
   wills

   ;; A journey is somewhere the villager his heading to.
   journey

   ;; A villager is somewhere in the village, and this
   ;; attribute just nails that.
   coordinates

   ;; We need to update villager according to time
   ;; (coordinates, hydration-level, ...)
   ;; Two models are possible. We either track the last
   ;; time the villager has been updated or we assume
   ;; the villager is updated on a fixed regular frequency.
   ;; The former model allow the game to skip some update
   ;; of the simulation to do more important things. If too
   ;; much time elapse between two updates of dehydration
   ;; level, the villager may suddenly die without having
   ;; the opportunity to quench his thirst.
   ;; The latter defines an atomic unit of time, nothing
   ;; is done between two updates. That means that's the
   ;; simulation is always accurate, even if the game is
   ;; very slow.
   ;; I opt for time tracking solution. The game must be
   ;; fast enough to provide a simulation of quality.
   ;; Moreover, if the time-provider is not real (could
   ;; be a counter maintained by the game), we would end
   ;; up to the same result than fixed update frequency
   ;; solution.
   last-update
   ]

  Vital
  (update-vitals
    [this {:keys [seconds->ticks time-provider] :as conf}]
    ;; a villager will die of dehydration in 48h
    (let [dehydration-per-tick (/ 1 (seconds->ticks  (* 48 3600)))
          nb-ticks             (- (time-provider) last-update)
          dehydration          (* dehydration-per-tick nb-ticks)]
      (update this :hydration-level
              #(max 0 (- % dehydration)))))

  FreeWill
  (update-will
    [this]
    (cond->
      this
      ;; hydration level is too low so go to the well
      ;; (and there is nothing else to do in this shit hole)
      (thirsty? this) (willing-to :villager.will/drink))))

(defn spawn
  "Make a new idle villager whom is perfectly sain of mind
  and health."
  [{:keys [time-provider id-provider] :as _conf}]
  (map->Villager {:id              (id-provider)
                  :hydration-level 1.0
                  :wills           nil
                  :journey         nil
                  :coordinates     [1 1]
                  :last-update     (time-provider)}))

(defmulti will-solver
          "will try to find a journey to fulfill a will"
          (fn [villager _conf] (first (:wills villager))))

(defmethod will-solver :villager.will/drink
  [{:keys [coordinates] :as _villager}
   {:keys [path-finder]
    :as   conf}]
  (when-let [well-path (path-finder coordinates
                                    #{:village.factory/well})]
    (journey/build conf well-path)))

(defmethod will-solver :default [_ _] nil)

(defn track-last-update
  [villager {:keys [time-provider] :as _conf}]
  (assoc villager :last-update (time-provider)))

(defn update-villager
  [villager conf]
  (cond-> villager

          :always
          (update-vitals conf)

          (not (on-journey? villager))
          (-> (update-will)
              (new-journey #(will-solver % conf)))

          :finally
          (track-last-update conf)))