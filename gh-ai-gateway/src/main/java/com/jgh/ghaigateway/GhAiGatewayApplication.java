package com.jgh.ghaigateway;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;

// 在common依赖中引入了数据库，这里需要排除掉，因为网关没有配置数据库连接信息，但是数据库依赖会有自动装配
// 所以需要排除掉，不然会报错
@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
})
@EnableDubbo
public class GhAiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GhAiGatewayApplication.class, args);
	}

}
