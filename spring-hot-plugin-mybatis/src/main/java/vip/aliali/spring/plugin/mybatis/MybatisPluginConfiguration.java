package vip.aliali.spring.plugin.mybatis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPluginConfiguration {

    @Bean
    @ConditionalOnClass(name = "org.apache.ibatis.session.SqlSession")
    public RegisterMybatis createRegisterMybatis(ApplicationContext main) {
        return new RegisterMybatis(main);
    }

    /**
     * 用来处理MybatisPlus
     * @return MybatisPlus的处理工具
     */
    @Bean
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.core.MybatisConfiguration")
    public MybatisPlusUtil createMybatisPluginAutoConfiguration() {
        return new MybatisPlusUtil();
    }
}
