(ns org.rssys.util.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [matcho.core :refer [match]]
            [org.rssys.util.core :as sut]))


(deftest ^:unit a-test
  (testing "simple test."
    (is (= 1 1))))
