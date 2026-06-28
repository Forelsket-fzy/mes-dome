package com.fzy.mes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
		"com.fzy.mes.module.auth.mapper",
		"com.fzy.mes.module.workorder.mapper",
		"com.fzy.mes.module.dispatch.mapper",
		"com.fzy.mes.module.report.mapper",
		"com.fzy.mes.module.integration.mapper",
		"com.fzy.mes.module.quality.mapper"
})
public class MesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MesApplication.class, args);
	}

}
