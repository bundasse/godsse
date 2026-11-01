package com.godsse.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.domain.RuleSystem;
import com.godsse.backend.repository.RuleSystemRepository;

/**
 * 룰 자동완성 목록의 기본값을 채운다.
 *
 * <p>
 * <b>이미 있는 약어(code)는 건드리지 않는다.</b> 나중에 이름을 고쳤거나 잠시 감춘 룰을
 * 재시작할 때마다 되돌리면 안 되기 때문이다. 그래서 몇 번을 실행해도 결과가 같다(멱등).
 *
 * <p>
 * 데이터 파일({@code data.sql}) 대신 시작 시점 코드로 넣는 이유: 테이블은 JPA 가 만들기 때문에
 * SQL 파일은 "스키마가 만들어진 뒤" 실행되도록 순서를 맞춰야 하고, 테스트 DB(create-drop)마다
 * 같은 설정을 또 넣어야 한다. 시작 코드는 그런 순서 문제가 없다.
 */
@Component
public class RuleSystemSeeder implements ApplicationRunner {

	/** 약어(code) → 화면에 보이는 이름(label). 목록 순서가 자동완성 순서가 된다. */
	private record Seed(String code, String label, int sortOrder) {
	}

	private static final List<Seed> DEFAULTS = List.of(new Seed("COC7", "크툴루의 부름 7판", 10),
			new Seed("COC6", "크툴루의 부름 6판", 20), new Seed("DND5E", "던전 앤 드래곤 5판", 30),
			new Seed("DW", "던전월드", 40), new Seed("INSANE", "인세인", 50),
			new Seed("MAGICA", "마기카로기아", 60), new Seed("SHINOBIGAMI", "시노비가미", 70),
			new Seed("PHANTOM", "팬텀 블러드", 80));

	private final RuleSystemRepository ruleSystemRepository;

	public RuleSystemSeeder(RuleSystemRepository ruleSystemRepository) {
		this.ruleSystemRepository = ruleSystemRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (Seed seed : DEFAULTS) {
			if (this.ruleSystemRepository.findByCode(seed.code()).isEmpty()) {
				this.ruleSystemRepository.save(new RuleSystem(seed.code(), seed.label(), seed.sortOrder()));
			}
		}
	}

}
