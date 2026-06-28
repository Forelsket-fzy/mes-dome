package com.fzy.mes;

import com.fzy.mes.module.auth.entity.UserAuth;
import com.fzy.mes.module.auth.mapper.SysUserMapper;
import com.fzy.mes.module.auth.mapper.UserAuthMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private UserAuthMapper userAuthMapper;

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void sysUserMapperCanQueryDatabase() {
		assertTrue(sysUserMapper.selectCount(null) >= 0);
	}



}
