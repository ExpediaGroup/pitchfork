---
id: kubernetes
title: Kubernetes
---

## Kubernetes

If you are using Kubernetes you have two options to install Pitchfork: with Helm, or by manually creating all of the individual Kubernetes resources.

### Helm

To install Pitchfork with Helm you need to add a new repository for Expedia Group and then you can simply run a helm install.

```bash
helm repo add expediagroup http://expediagroup.github.io/helm-charts/repo
helm install pitchfork expediagroup/pitchfork
```

If you need to override the default options you can find the available settings [here](https://github.com/ExpediaGroup/helm-charts/blob/master/charts/pitchfork/values.yaml).

### Individual resources

To install the individual Pitchfork resources you can use the following scripts.

#### Deployment

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
      containers:
        # Please replace "latest" with the most recent version available at https://hub.docker.com/r/expediagroup/pitchfork/tags
        - name: pitchfork
          image: expediagroup/pitchfork:latest
          ports:
            - containerPort: 9411
            - containerPort: 8081
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: SERVER_PORT
              value: "9411"
            # If you are familiar with the JVM you can tune memory and settings here. If not, these should give you an overall decent experience.
            - name: JAVA_JVM_ARGS
              value: "-XX:MaxRAMPercentage=80.0"
            # You can enabled and configure more forwarders here.
            - name: PITCHFORK_FORWARDERS_LOGGING_ENABLED
              value: "true"
            # You can enable and configure more forwarders here.
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_ENABLED
              value: "true"
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-service:9092"
            # The following properties are use to tag metrics produced by Pitchfork with the app name and with the name of the pod.
            - name: MANAGEMENT_METRICS_TAGS_APP
              value: "pitchfork"
            - name: MANAGEMENT_METRICS_TAGS_INSTANCE
              value: $(POD_NAME)
            - name: MANAGEMENT_METRICS_EXPORT_GRAPHITE_TAGS_AS_PREFIX
              value: "app,instance"
            # Isolating actuator endpoints. This allows Pitchfork to handle healthchecks even when under extremely high load.
            - name: MANAGEMENT_SERVER_PORT
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
              path: /info
              port: 8081
            initialDelaySeconds: 10
            timeoutSeconds: 1
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /health
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

#### Service

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

#### Horizontal Pod Autoscaler

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
