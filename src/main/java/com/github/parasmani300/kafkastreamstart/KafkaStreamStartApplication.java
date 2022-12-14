package com.github.parasmani300.kafkastreamstart;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Properties;

@SpringBootApplication
public class KafkaStreamStartApplication {

	public Topology createTopology(){
		StreamsBuilder builder = new StreamsBuilder();
		// 1 - stream from Kafka


		KStream<String, String> textLines = builder.stream("word-count-input");
		KTable<String, Long> wordCounts = textLines
				// 2 - map values to lowercase
				.mapValues(textLine -> textLine.toLowerCase())
				// can be alternatively written as:
				// .mapValues(String::toLowerCase)
				// 3 - flatmap values split by space
				.flatMapValues(textLine -> Arrays.asList(textLine.split("\\W+")))
				// 4 - select key to apply a key (we discard the old key)
				.selectKey((key, word) -> word)
				// 5 - group by key before aggregation
				.groupByKey()
				// 6 - count occurences
				.count(Materialized.as("Counts"));

		// 7 - to in order to write the results back to kafka
		wordCounts.toStream().to("word-count-output", Produced.with(Serdes.String(), Serdes.Long()));

		return builder.build();
	}


	public static void main(String[] args) {

		SpringApplication.run(KafkaStreamStartApplication.class, args);

		Properties config = new Properties();
		config.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
		config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

		KafkaStreamStartApplication kafkaStreamStartApplication = new KafkaStreamStartApplication();

		KafkaStreams kafkaStreams = new KafkaStreams(kafkaStreamStartApplication.createTopology(),config);
		kafkaStreams.start();

		System.out.println(kafkaStreams.toString());

		Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));



	}

}
