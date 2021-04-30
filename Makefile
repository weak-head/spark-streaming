PRODUCER_IMAGE_ID ?= github.com/weak-head/spark-streaming/producer
PRODUCER_IMAGE_TAG ?= 0.0.1

PROCESSOR_IMAGE_ID ?= github.com/weak-head/spark-streaming/processor
PROCESSOR_IMAGE_TAG ?= 0.0.1


.PHONY: build-docker-producer
build-docker-producer:
	cd src/producer && docker build -t $(PRODUCER_IMAGE_ID):$(PRODUCER_IMAGE_TAG) .


.PHONY: build-docker-processor
build-docker-processor:
	cd src/processor && docker build -t $(PROCESSOR_IMAGE_ID):$(PROCESSOR_IMAGE_TAG) .


.PHONY: build-docker
build-docker: build-docker-producer build-docker-processor


.PHONY: load-images
load-images:
	kind load docker-image $(PRODUCER_IMAGE_ID):$(PRODUCER_IMAGE_TAG)
	kind load docker-image $(PROCESSOR_IMAGE_ID):$(PROCESSOR_IMAGE_TAG)


.PHONY: deploy
deploy: build-docker load-images
	kubectl create -f deployment/producer-p1-deployment.yaml
	kubectl create -f deployment/producer-p2-deployment.yaml
	kubectl create -f deployment/processor-p1-deployment.yaml

.PHONY: clean
clean:
	kubectl delete -f deployment/producer-p1-deployment.yaml
	kubectl delete -f deployment/producer-p2-deployment.yaml
	kubectl delete -f deployment/processor-p1-deployment.yaml