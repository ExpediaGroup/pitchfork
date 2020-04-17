---
id: kubernetes
title: Kubernetes
---

## Kubernetes

If you are using Kubernetes you may find the following scripts useful for a new installation of Pitchfork.

They consist of a deployment, a service and a horizontal pod autoscaler that you can modify according to your needs.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pitchfork
  labels:
    app: pitchfork
spec:
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
  replicas: 1
  selector:
    matchLabels:
      app: pitchfork
  template:
    metadata:
      labels:
        app: pitchfork
        release: pitchfork
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      terminationGracePeriodSeconds: 0
      containers:
        - name: pitchfork
          image: expediagroup/pitchfork:1.19
          ports:
            - containerPort: 9411
              name: pitchfork
            - containerPort: 8081
              name: actuator
          env:
            - name: SERVER_PORT
              value: "9411"
            - name: JAVA_JVM_ARGS
              value: "-Dspring.jmx.enabled=true -XX:MaxRAMPercentage=80.0"
            - name: PITCHFORK_FORWARDERS_LOGGING_ENABLED
              value: "true"
            # You can enabled and configure more forwarders here.
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_ENABLED
              value: "true"
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-service:9092"
            # The following properties are use to tag metrics produced by Pitchfork with the app name and with the name of the pod.
            - name: MANAGEMENT_METRICS_TAGS_APP
              value: "pitchfork"
            - name: MANAGEMENT_METRICS_TAGS_INSTANCE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: MANAGEMENT_METRICS_EXPORT_GRAPHITE_TAGS_AS_PREFIX
              value: "APP,INSTANCE"
            # Isolating actuator endpoints. This allows Pitchfork to handle healthchecks even when under immense load.
            - name: MANAGEMENT_ENDPOINT_SERVER_PORT
              value: "8081"
          resources:
            requests:
              memory: "1Gi"
              cpu: "1"
            limits:
              memory: "1Gi"
              cpu: "1"
          livenessProbe:
            httpGet:
              path: /actuator/info
              port: 8081
            initialDelaySeconds: 10
            timeoutSeconds: 1
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 10
            timeoutSeconds: 1
            periodSeconds: 30
          # Artificial sleep to allow connections to drain
          lifecycle:
            preStop:
              exec:
                command: [ "/bin/sleep", "20"]
```


```yaml
apiVersion: v1
kind: Service
metadata:
  name: pitchfork
  labels:
    app: pitchfork
spec:
  type: ClusterIP
  ports:
    - name: app-port
      port: 9411
      protocol: TCP
      targetPort: 9411
```


```yaml
apiVersion: autoscaling/v2beta2
kind: HorizontalPodAutoscaler
metadata:
  name: pitchfork
  labels:
    app: pitchfork
spec:
  scaleTargetRef:
    apiVersion: extensions/v2beta1
    kind: Deployment
    name: pitchfork
  minReplicas: 1
  maxReplicas: 3
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 30
```
