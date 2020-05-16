;; Copyright (c) Mikhail Ananev, 2020.
;; Red Stars Systems (https://rssys.org).
;; Distributed under the Eclipse Public License 2.0 or (at your option) any later version.

(ns org.rssys.util.jvm
  "Utility helpers for JVM."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.lang.management ManagementFactory ThreadInfo ThreadMXBean)
           (com.sun.management HotSpotDiagnosticMXBean)
           (java.io File)
           (java.text SimpleDateFormat)
           (java.util Date)))

(def ^:const HOTSPOT_BEAN_NAME "com.sun.management:type=HotSpotDiagnostic")

(defn heap-dump
  "Perform heap dump for current JVM process.
  Params:
  * filename - String.
  * live? - if true then catch only live objects (throw away garbage), if false then catch all objects.
  Returns: {:filename string? :size pos-int?}"
  [^String out-filename ^Boolean live?]
  (let [out-filename (if (str/ends-with? out-filename ".hprof")
                       out-filename
                       (str out-filename ".hprof"))
        server       (ManagementFactory/getPlatformMBeanServer)
        bean         ^HotSpotDiagnosticMXBean (ManagementFactory/newPlatformMXBeanProxy server HOTSPOT_BEAN_NAME HotSpotDiagnosticMXBean)]
    (.dumpHeap bean out-filename live?)
    {:filename out-filename :size (.length ^File (io/file out-filename))}))

(defn- thread-info
  [^ThreadMXBean mxbean ^ThreadInfo ti]
  (format "\"%s\" id=%d prio=%s state=%s native=%s daemon=%s suspended=%s block=%s wait=%s
  \t\tlock=%s cpu=%s user=%s\n
  %s
  Locked ownable synchronizers:\n%s\n
  ---------------------------------------------------------------------------------------------------\n\n"
    (.getThreadName ti)
    (.getThreadId ti)
    (.getPriority ti)
    (.getThreadState ti)
    (.isInNative ti)
    (.isDaemon ti)
    (.isSuspended ti)
    (.getBlockedCount ti)
    (.getWaitedCount ti)
    (.getLockName ti)
    (quot (.getThreadCpuTime mxbean (.getThreadId ti)) 1000000)
    (quot (.getThreadUserTime mxbean (.getThreadId ti)) 1000000)
    (apply str (map #(str "\t\t" % "\n") (.getStackTrace ti)))
    (apply str (map #(str "\t\t" "- (a " (.toString %) ")\n") (.getLockedSynchronizers ti)))))

(defn thread-dump-jvm
  "Perform thread dump for current JVM process.
  Returns: {:filename string? :size pos-int?} if out-filename is specified
  {:dump string? :size pos-int?} - if no arg is specified."
  ([]
   (let [bean   (ManagementFactory/getThreadMXBean)
         dump   (.dumpAllThreads bean true true)
         header (str
                  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date.))
                  \newline
                  (format "Thread dump of %s (%s %s)"
                    (System/getProperty "java.vm.name")
                    (System/getProperty "java.vm.version")
                    (System/getProperty "java.vm.info"))
                  \newline
                  \newline)
         result (str header (apply str (mapv #(thread-info bean %) dump)))]
     {:dump result :size (.length result)}))
  ([^String out-filename]
   (let [out-filename (if (str/ends-with? out-filename ".txt")
                        out-filename
                        (str out-filename ".txt"))
         result       (thread-dump-jvm)]
     (spit out-filename (:dump result))
     {:filename out-filename :size (.length ^File (io/file out-filename))})))

(comment
  (heap-dump "abcd" true)
  (thread-dump-jvm "cc.txt"))



