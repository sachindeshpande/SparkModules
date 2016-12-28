package com.cloudera.sparkwordcount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.SparkConf;
import scala.Tuple2;

public class TestSparkS3 {
  public static void main(String[] args) {

    // create Spark context with Spark configuration
    JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("Spark Count"));
        sc.hadoopConfiguration().set("fs.s3a.access.key", "AKIAIYQDZNMYOUJ54AKQ");
        sc.hadoopConfiguration().set("fs.s3a.secret.key", "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");
    // get threshold
    final int threshold = Integer.parseInt(args[1]);

    JavaRDD<String> tokenized  = sc.textFile("s3a://spark-s3-bucket-sacd/MyObjectKey2").flatMap(
      new FlatMapFunction<String,String>() {
        @Override
        public Iterable call(String s) {
          return Arrays.asList(s.split(" "));
        }
      }
    );
    
    // count the occurrence of each word
    JavaPairRDD<String, Integer> counts = tokenized.mapToPair(
      new PairFunction<String,String,Integer>() {
        public Tuple2 call(String s) {
          return new Tuple2(s, 1);
        }
      }
    ).reduceByKey(
      new Function2<Integer,Integer,Integer>() {
        @Override
        public Integer call(Integer i1, Integer i2) {
          return i1 + i2;
        }
      }
    );

    // filter out words with fewer than threshold occurrences
    JavaPairRDD<String, Integer> filtered = counts.filter(
      new Function<Tuple2<String,Integer>,Boolean>() {
        @Override
        public Boolean call(Tuple2 tup) {
          return Integer.parseInt(tup._2.toString()) >= threshold;
        }
      }
    );

    // count characters
    JavaPairRDD<Character, Integer> charCounts = filtered.flatMap(
      new FlatMapFunction<Tuple2<String, Integer>, Character>() {
        @Override
        public Iterable<Character> call(Tuple2<String, Integer> s) {
          Collection<Character> chars = new ArrayList<Character>(s._1().length());
          for (char c : s._1().toCharArray()) {
            chars.add(c);
          }
          return chars;
        }
      }
    ).mapToPair(
      new PairFunction<Character, Character, Integer>() {
        @Override
        public Tuple2<Character, Integer> call(Character c) {
          return new Tuple2<Character, Integer>(c, 1);
        }
      }
    ).reduceByKey(
      new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer i1, Integer i2) {
          return i1 + i2;
        }
      }
    );

    System.out.println(charCounts.collect());
  }
}
