package main

import (
	"context"
	"fmt"
	"log"
	"math/rand"
	"net"
	"os"
	"strconv"
	"time"

	"github.com/google/uuid"
	kafka "github.com/segmentio/kafka-go"
)

func newKafkaWriter(kafkaURL, topic string) *kafka.Writer {
	return &kafka.Writer{
		Addr:     kafka.TCP(kafkaURL),
		Topic:    topic,
		Balancer: &kafka.LeastBytes{},
	}
}

func createTopic(kafkaURL, topic string) {
	conn, err := kafka.Dial("tcp", kafkaURL)
	if err != nil {
		panic(err.Error())
	}
	defer conn.Close()

	controller, err := conn.Controller()
	if err != nil {
		panic(err.Error())
	}
	var controllerConn *kafka.Conn
	controllerConn, err = kafka.Dial("tcp", net.JoinHostPort(controller.Host, strconv.Itoa(controller.Port)))
	if err != nil {
		panic(err.Error())
	}
	defer controllerConn.Close()

	topicConfigs := []kafka.TopicConfig{{
		Topic:             topic,
		NumPartitions:     1,
		ReplicationFactor: 1,
	}}

	err = controllerConn.CreateTopics(topicConfigs...)
	if err != nil {
		panic(err.Error())
	}
}

func produce(w *kafka.Writer, prefix string) {
	rndSource := rand.NewSource(time.Now().UnixNano())
	r := rand.New(rndSource)

	for i := 0; ; i++ {
		log.Printf("sending a new message: %d", i)

		msg := kafka.Message{
			Key:   []byte(fmt.Sprintf("%s-key-%d", prefix, i)),
			Value: []byte(fmt.Sprintf("%s", uuid.New())),
		}

		err := w.WriteMessages(context.Background(), msg)
		if err != nil {
			log.Println(err)
		}

		sleepDelay := time.Duration(500 + r.Intn(500))
		time.Sleep(sleepDelay * time.Millisecond)
	}
}

func main() {
	kafkaURL := os.Getenv("kafkaHost")
	topic := os.Getenv("kafkaTopic")
	prefix := os.Getenv("producerPrefix")

	createTopic(kafkaURL, topic)

	writer := newKafkaWriter(kafkaURL, topic)
	defer writer.Close()

	produce(writer, prefix)
}
