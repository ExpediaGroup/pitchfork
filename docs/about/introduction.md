---
id: introduction
title: Introduction
---

<h1 align="left">
  <img width="300" alt="Pitchfork" src="../assets/pitchfork_logo.svg" />
</h1>

Pitchfork lifts Zipkin tracing data into Haystack.

## The Problem

[Haystack](https://expediadotcom.github.io/haystack/) is an Expedia-backed project to facilitate detection and remediation of problems with enterprise-level web services and websites. Much like Zipkin, its primary goal is to provide an easy to use UI to analyse distributed tracing data, but it offers other features like trend analysis or adaptive alerting.

[Zipkin](http://zipkin.io) is the de facto standard for distributed tracing. We understand that migrating to a new system can be difficult and you may want to go back. Pitchfork can help you with this.

## The Solution

Pitchfork lifts Zipkin tracing data into Haystack.

Among others, Pitchfork:
- Supports different ingresses (HTTP, Kafka, and RabbitMQ)
- Supports different outputs (Kafka, Kinesis, and Zipkin HTTP)
- Allows logging of spans
- Reports metrics to endpoints or/and to a Graphite collector
- Exposes endpoints that can be used to test the app's health and to retrieve useful info

