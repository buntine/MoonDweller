(ns macros.core)

(defmacro dotrue [& body]
  (conj
    (cons
      (cons 'do body) '(true))
   'do))
