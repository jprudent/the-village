(ns the-village.storage)

(defprotocol Storage
  (gather [storage n]
    "retrieve n goods from storage.
    IN : - n : the number of good to be retrieved
    OUT : - success : [storage-updated goods]
          - failure : [storage failure]")
  (store [storage good]
    "stock a good.
    IN : - good : the good to store
    OUT : - success : [storage-updated]
          - failure : [storage failure]"))