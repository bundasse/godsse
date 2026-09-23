package com.godsse.backend.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.EchoRequest;
import com.godsse.backend.api.dto.EchoResponse;
import com.godsse.backend.api.dto.HelloResponse;

import jakarta.validation.Valid;

/**
 * 프론트엔드 연동 확인용 샘플 API.
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

	@GetMapping
	public HelloResponse hello() {
		return new HelloResponse("godsse 백엔드에 연결되었습니다.", Instant.now());
	}

	@GetMapping("/{name}")
	public HelloResponse helloTo(@PathVariable String name) {
		return new HelloResponse(name + "님, 반갑습니다!", Instant.now());
	}

	@PostMapping("/echo")
	public EchoResponse echo(@Valid @RequestBody EchoRequest request) {
		String message = request.message().trim();
		return new EchoResponse(request.name(), message, message.length());
	}

}
