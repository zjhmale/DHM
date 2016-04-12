(ns hm.algs-test
  (require [acolfut.sweet :refer :all]
           [hm.syntax :refer :all]
           [hm.env :refer :all]
           [hm.algs :refer :all]))

(deftest algs-test
  (testing "inference normal types"
    (let [fun-id    (EAbs 0 (EVar 0))
          fun-true  (EAbs 0 (EAbs 1 (EVar 0)))
          fun-false (EAbs 0 (EAbs 1 (EVar 1)))
          e-true    (ELit (LBool true))
          e-false   (ELit (LBool false))
          e-1       (ELit (LInt 1))
          e-2       (ELit (LInt 2))
          expr0     (ELet 0 fun-id (EApp (EApp fun-false (EVar 0))
                                         (EApp (EApp (EVar 0) (EVar 0)) e-false)))
          expr1     (ELet "id" (EAbs "x" (EVar "x")) (EVar "id"))
          expr2     (ELet "id" (EAbs "x" (EVar "x")) (EApp (EVar "id") (EVar "id")))
          expr3     (ELet "id"
                          (EAbs "x" (ELet "y" (EVar "x") (EVar "y")))
                          (EApp (EVar "id") (EVar "id")))
          expr4     (ELet "id"
                          (EAbs "x" (ELet "y" (EVar "x") (EVar "y")))
                          (EApp (EApp (EVar "id") (EVar "id")) (ELit (LInt 3))))
          expr5     (ELet "id"
                          (EAbs "x" (EApp (EVar "x") (EVar "x")))
                          (EVar "id"))
          expr6     (EAbs "m" (ELet "y"
                                    (EVar "m")
                                    (ELet "x"
                                          (EApp (EVar "y") (ELit (LBool true)))
                                          (EVar "x"))))
          expr7     (EApp (ELit (LInt 3)) (ELit (LInt 3)))
          expr8     (EAbs "a"
                          (ELet "x"
                                (EAbs "b"
                                      (ELet "y"
                                            (EAbs "c" (EApp (EVar "a") (ELit (LInt 1))))
                                            (EApp (EVar "y") (ELit (LInt 3)))))
                                (EApp (EVar "x") (ELit (LInt 3)))))
          expr9     (EAbs "a" (EAbs "b"
                                    (EApp (EVar "b")
                                          (EApp (EVar "a")
                                                (EApp (EVar "a") (EVar "b"))))))
          expr10    (ELet "g"
                          (EAbs "f" (ELit (LInt 5)))
                          (EApp (EVar "g") (EVar "g")))
          ;; λf -> λg -> λarg -> g (f arg)
          expr11    (EAbs "f"
                          (EAbs "g"
                                (EAbs "arg"
                                      (EApp (EVar "g")
                                            (EApp (EVar "f") (EVar "arg"))))))
          expr12    (EId (EId (ELit (LInt 3))))
          expr13    (ECompose (EAbs "x" (ENot (EVar "x")))
                              (EAbs "x" (EEq (ELit (LInt 3)) (EVar "x"))))
          expr14    (EAdd (ELit (LBool true))
                          (ELit (LBool false)))
          ;; compose1 (b -> c) -> ((a -> b) -> (a -> c))
          ;; compose2 (e -> f) -> ((d -> e) -> (d -> f))
          ;; just substitution game
          expr15    (EAbs "x"
                          (ECompose (EAbs "y"
                                          (EAbs "z"
                                                (ECompose (EVar "y") (EVar "z"))))
                                    (EVar "x")))
          expr16    (EAnd (ELit (LBool true))
                          (ELit (LBool false)))]
      (is= (s-of-m (infer {} fun-id))
           "a -> a")
      (is= (s-of-m (infer {} fun-true))
           "a -> (b -> a)")
      (is= (s-of-m (infer {} fun-false))
           "a -> (b -> b)")
      (is= (s-of-m (infer {} e-true))
           (s-of-m (infer {} e-false))
           "bool")
      (is= (s-of-m (infer {} e-1))
           (s-of-m (infer {} e-2))
           "int")
      (is= (s-of-m (infer {} expr0)) "bool")
      (is= (s-of-m (infer {} expr1)) "b -> b")
      (is= (s-of-m (infer {} expr2)) "c -> c")
      (is= (s-of-m (infer {} expr3)) "c -> c")
      (is= (s-of-m (infer {} expr4)) "int")
      (is= (s-of-m (infer {} expr5))
           "occurs check fails: a vs. a -> b in let id = λx -> x x in id")
      (is= (s-of-m (infer {} expr6)) "(bool -> b) -> b")
      (is= (s-of-m (infer {} expr7))
           "types do not unify: int vs. int -> a in 3 3")
      (is= (s-of-m (infer {} expr8)) "(int -> h) -> h")
      (is= (s-of-m (infer {} expr9))
           "occurs check fails: d vs. d -> e in λa -> λb -> b (a (a b))")
      (is= (s-of-m (infer {} expr10)) "int")
      (is= (s-of-m (infer {} expr11))
           "(c -> d) -> ((d -> e) -> (c -> e))")
      (is= (s-of-m (infer common-env expr12)) "int")
      (is= (s-of-m (infer common-env expr13)) "int -> bool")
      (is= (s-of-m (infer common-env expr14)) "types do not unify: bool vs. int in true + false")
      (is= (s-of-m (infer common-env expr15)) "(b -> (h -> i)) -> (b -> ((g -> h) -> (g -> i)))")
      (is= (s-of-m (infer common-env expr16)) "bool")))
  (testing "inference compound types"
    (let [expr0  (EPair (ELit (LInt 3))
                        (ELit (LBool true)))
          expr1  (EPair (EApp (EVar "f") (ELit (LInt 3)))
                        (EApp (EVar "f") (ELit (LInt 3))))
          expr2  (EAbs "f"
                       (EPair (EApp (EVar "f") (ELit (LInt 3)))
                              (EApp (EVar "f") (ELit (LInt 3)))))
          expr3  (EAbs "f"
                       (EPair (EApp (EVar "f") (ELit (LInt 3)))
                              (EApp (EVar "f") (ELit (LBool true)))))
          expr4  (ELet "f"
                       (EAbs "x" (EVar "x"))
                       (EPair (EApp (EVar "f") (ELit (LInt 3)))
                              (EApp (EVar "f") (ELit (LBool true)))))
          expr5  (EAbs "g"
                       (ELet "f"
                             (EAbs "x" (EVar "g"))
                             (EPair (EApp (EVar "f") (ELit (LInt 3)))
                                    (EApp (EVar "f") (ELit (LBool true))))))
          ;; let rec len = λxs -> if (isempty xs) 0 (succ (len (tail xs))) in len (cons 0 nil)
          expr6  (ELetRec "len"
                          (EAbs "xs"
                                (EIf (EIsEmpty (EVar "xs"))
                                     (ELit (LInt 0))
                                     (ESucc (EApp (EVar "len")
                                                  (ETail (EVar "xs"))))))
                          (EApp (EVar "len")
                                (ECons (ELit (LInt 0)) ENil)))
          expr7  (ELetRec "len"
                          (EAbs "xs"
                                (EIf (EIsEmpty (EVar "xs"))
                                     (ELit (LInt 0))
                                     (ESucc (EApp (EVar "len")
                                                  (ETail (EVar "xs"))))))
                          (EVar "len"))
          ;; let-polymorphism, prenex polymorphism or more generally rank-1 polymorphism
          expr8  (ELet "f"
                       (EAbs "x" (EVar "x"))
                       (ELet "p"
                             (EPair (EApp (EVar "f") (ELit (LInt 3)))
                                    (EApp (EVar "f") (ELit (LBool true))))
                             (EVar "p")))
          ;; not allow polymorphic lambda abstraction
          expr9  (EAbs "id"
                       (EPair (EApp (EVar "id") (ELit (LBool true)))
                              (EApp (EVar "id") (ELit (LInt 3)))))
          expr10 (EPair (ELit (LString "term"))
                        (ELit (LInt 3)))]
      (is= (s-of-m (infer common-env expr0)) "(int * bool)")
      (is= (s-of-m (infer common-env expr1)) "unbound variable: f in (f 3, f 3)")
      (is= (s-of-m (infer common-env expr2)) "(int -> c) -> (c * c)")
      (is= (s-of-m (infer common-env expr3)) "types do not unify: int vs. bool in λf -> (f 3, f true)")
      (is= (s-of-m (infer common-env expr4)) "(int * bool)")
      (is= (s-of-m (infer common-env expr5)) "f -> (f * f)")
      (is= (s-of-m (infer common-env expr6)) "int")
      (is= (s-of-m (infer common-env expr7)) "[e] -> int")
      (is= (s-of-m (infer common-env expr8)) "(int * bool)")
      (is= (s-of-m (infer common-env expr9)) "types do not unify: bool vs. int in λid -> (id true, id 3)")
      (is= (s-of-m (infer {} expr10)) "(string * int)")))
  (testing "inference recursive function types"
    (let [expr0 (ELetRec "factorial"
                         (EAbs "n"
                               (EIf (EIsZero (EVar "n"))
                                    (ELit (LInt 1))
                                    (ETimes (EVar "n")
                                            (EApp (EVar "factorial")
                                                  (EPred (EVar "n"))))))
                         (EVar "factorial"))
          expr1 (ELetRec "factorial"
                         (EAbs "n"
                               (EIf (EIsZero (EVar "n"))
                                    (ELit (LInt 1))
                                    (ETimes (EVar "n")
                                            (EApp (EVar "factorial")
                                                  (EPred (EVar "n"))))))
                         (EApp (EVar "factorial") (ELit (LInt 5))))
          ;; letrec is just a suger of let and fix point combinator
          expr2 (ELet "factorial"
                      (EFix (EAbs "factorial"
                                  (EAbs "n"
                                        (EIf (EIsZero (EVar "n"))
                                             (ELit (LInt 1))
                                             (ETimes (EVar "n")
                                                     (EApp (EVar "factorial")
                                                           (EPred (EVar "n"))))))))
                      (EVar "factorial"))
          expr3 (ELet "factorial"
                      (EFix (EAbs "factorial"
                                  (EAbs "n"
                                        (EIf (EIsZero (EVar "n"))
                                             (ELit (LInt 1))
                                             (ETimes (EVar "n")
                                                     (EApp (EVar "factorial")
                                                           (EPred (EVar "n"))))))))
                      (EApp (EVar "factorial") (ELit (LInt 5))))]
      (is= (s-of-m (infer common-env expr0)) "int -> int")
      (is= (s-of-m (infer common-env expr1)) "int")
      (is= (s-of-m (infer common-env expr2)) "int -> int")
      (is= (s-of-m (infer common-env expr3)) "int"))))
