open module haystack.proxy {
    requires spring.web;
    requires kafka.clients;
    requires javax.inject;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires protobuf.java;
    requires java.sql;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.webflux;
    requires jackson.annotations;
    requires reactor.core;
    requires reactor.netty;
    requires io.netty.codec.http;
    requires org.reactivestreams;
    requires io.netty.transport;
    requires io.netty.codec;
    requires slf4j.api;
    requires zipkin2;
    requires zipkin2.reporter;
    requires zipkin2.reporter.okhttp3;
    requires com.fasterxml.jackson.databind;
    requires okhttp3;
}
