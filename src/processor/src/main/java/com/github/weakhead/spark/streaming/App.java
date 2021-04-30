package com.github.weakhead.spark.streaming;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import scala.Tuple2;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.streaming.Durations;

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 */

public final class App {
  private static final Pattern SPACE = Pattern.compile(" ");

  public static void main(String[] args) throws Exception {
    String kafkaHost = System.getenv("kafkaHost");
    String kafkaTopics = System.getenv("kafkaTopics");
    String kafkaGroupId = System.getenv("kafkaGroupId");
    String sparkHost = System.getenv("sparkHost");
    String sparkApp = System.getenv("sparkApp");

    // -- Spark config
    SparkConf sparkConf = new SparkConf();
		// sparkConf.set("spark.cassandra.connection.host", cassandraHost);
    // sparkConf.set("spark.cassandra.connection.port", cassandraPort);
    sparkConf.setMaster(sparkHost);
    sparkConf.setAppName(sparkApp);
    sparkConf.set("spark.testing.memory", "2147480000");

    // 2 seconds batch interval
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));

    // -- Kafka config
    Set<String> topicsSet = new HashSet<>(Arrays.asList(kafkaTopics.split(",")));
    Map<String, Object> kafkaParams = new HashMap<>();
    kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
    kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
    kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    // Create direct kafka stream with brokers and topics
    JavaInputDStream<ConsumerRecord<String, String>> messages = KafkaUtils.createDirectStream(
        jssc,
        LocationStrategies.PreferConsistent(),
        ConsumerStrategies.Subscribe(topicsSet, kafkaParams));

    // Get the lines, split them into words, count the words and print
    JavaDStream<String> lines = messages.map(ConsumerRecord::value);
    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
    JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
        .reduceByKey((i1, i2) -> i1 + i2);
    wordCounts.print();

    // Start the computation
    jssc.start();
    jssc.awaitTermination();
  }
}