package com.hx.vr.ch.gateway.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class CommonController {

	@GetMapping(path="/check",produces="text/plain")
	public Mono<String> check() {
		return Mono.just("ok");
	}
	
	@GetMapping(path="/favicon.ico",produces="text/plain")
	public Mono<String> favicon() {
		return Mono.just("ok");
	}
}
