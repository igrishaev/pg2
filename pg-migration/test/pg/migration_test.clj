(ns pg.migration-test
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [pg.migration.core :as mig]))


(deftest test-parse-file

  (let [res
        (-> "foo/bar/baz/0001-some-slug.next.sql"
            io/file
            mig/parse-file)]

    (is (= {:id 1, :slug "some slug", :direction :next}
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/20221211235559-Hello World .Next.SQL"
            io/file
            mig/parse-file)]

    (is (= {:slug "Hello World",
            :id 20221211235559
            :direction :next}
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/1aaa.prev.sql"
            io/file
            mig/parse-file)]

    (is (= {:id 1, :slug "aaa", :direction :prev}
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/999.prev.sql"
            io/file
            mig/parse-file)]

    (is (= {:id 999, :slug "", :direction :prev}
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/aaa.prev.sql"
            io/file
            mig/parse-file)]

    (is (= nil
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/555aaa..sql"
            io/file
            mig/parse-file)]

    (is (= nil
           (dissoc res :file))))

  (let [res
        (-> "foo/bar/baz/555aaa.prev.sql "
            io/file
            mig/parse-file)]

    (is (= nil
           (dissoc res :file)))))


(deftest test-validate-duplicates

  (let [res
        (mig/validate-duplicates!
         [{:id 1
           :direction :prev}
          {:id 1
           :direction :next}
          {:id 2
           :direction :prev}
          {:id 2
           :direction :next}
          {:id 3
           :direction :prev}
          {:id 4
           :direction :next}])]

    (is (vector? res)))

  (try
    (mig/validate-duplicates!
     [{:id 1
       :direction :prev
       :file (io/file "mig1a")}
      {:id 1
       :direction :next}
      {:id 2
       :direction :prev}
      {:id 2
       :direction :next}
      {:id 3
       :direction :prev}
      {:id 4
       :direction :next}
      {:id 1
       :direction :prev
       :file (io/file "mig1b")}])
    (is false)
    (catch Throwable e
      (is (= "Migration 1 has 2 prev files: mig1a, mig1b"
             (ex-message e))))))


(deftest test-make-file-name

  (is (= "123.hello-world.prev.sql"
         (mig/make-file-name 123 "hello-world" :prev)))

  (is (= "123.prev.sql"
         (mig/make-file-name 123 nil :prev)))

  (is (= "123.some-text-hello.prev.sql"
         (mig/make-file-name 123 "  \n some Text Hello \r\n" :prev)))

  (try
    (mig/make-file-name 123 nil :foobar)
    (is false)
    (catch Throwable e
      (is (= "No matching clause: :foobar"
             (ex-message e))))))


(deftest test-validate-conflicts

  (let [migrations
        [{:id 5
          :applied? true}
         {:id 6
          :applied? true}
         {:id 7
          :applied? false}]

        res
        (mig/validate-conflicts! migrations)]

    (is (nil? res)))

  (let [migrations
        []

        res
        (mig/validate-conflicts! migrations)]

    (is (nil? res)))

  (let [migrations
        [{:id 5
          :applied? true}
         {:id 6
          :applied? false}
         {:id 7
          :applied? true}]]

    (try
      (mig/validate-conflicts! migrations)
      (is false)
      (catch Throwable e
        (is (= "Migration conflict: migration 7 has been applied before 6"
               (ex-message e)))))))


(deftest test-create-migration-files

  (let [path
        "foo/bar/baz"

        res
        (mig/create-migration-files path "  A test migration \r\n ")

        [file-prev file-next]
        res]

    (is (-> file-prev
            str
            (str/starts-with? path)))

    (is (-> file-next
            str
            (str/starts-with? path)))

    (is (-> file-prev
            str
            (str/ends-with? ".a-test-migration.prev.sql")))

    (is (-> file-next
            str
            (str/ends-with? ".a-test-migration.next.sql")))

    (->> (io/file "foo")
         (file-seq)
         (reverse)
         (mapv io/delete-file))))
