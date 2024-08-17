package csdn.itsaysay.plugin.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPluginConfiguration {

    @Bean
    @ConditionalOnClass(SqlSession.class)
    public RegisterMybatis createRegisterMybatis(ApplicationContext main) {
        return new RegisterMybatis(main);
    }
}
