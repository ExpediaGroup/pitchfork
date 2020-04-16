---
id: kubernetes
title: Kubernetes
---

## Kubernetes

Bla bla. Explain motivation why we're adding this.
Most folks are using kube so this may be helpful.

Explain what this provides. deployment, hpa and service.

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: pitchfork
  labels:
    app: pitchfork
    release: pitchfork
spec:
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
  replicas: 1
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
          env:
            - name: SERVER_PORT
              value: "9411"
            - name: JAVA_JVM_ARGS
              value: "-Dspring.jmx.enabled=true -XMaxMemory=80%"
            - name: PITCHFORK_FORWARDERS_LOGGING_ENABLED
              value: "true"
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_ENABLED
              value: "true"
            - name: PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-service:9092"
            - name: MANAGEMENT_METRICS_TAGS_APP
              value: "pitchfork"
            - name: MANAGEMENT_METRICS_TAGS_INSTANCE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: MANAGEMENT_METRICS_EXPORT_GRAPHITE_TAGS_AS_PREFIX
              value: "APP,INSTANCE"
          resources:
            requests:
              memory: "1Gi"
              cpu: "1"
            limits:
              memory: "1Gi"
              cpu: "1"
          readinessProbe:
            httpGet:
              path: /health
              port: 9411
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
    release: pitchfork
spec:
  type: ClusterIP
  ports:
  - port: 9411
    name: web
  selector:
    app: pitchfork
    release: pitchfork
```


```yaml
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
  name: pitchfork
  labels:
    app: pitchfork
    release: pitchfork
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
      targetAverageUtilization: 30
```
