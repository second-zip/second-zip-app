package com.secondzip.backend.config;

import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@Configuration
@Log4j2
@MapperScan(
        basePackages = "com.secondzip.backend",
        annotationClass = org.apache.ibatis.annotations.Mapper.class
)
//Controller를 제외한 공통 Bean 등록
@ComponentScan(
        basePackages = "com.secondzip.backend",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = Controller.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = RestController.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = Configuration.class
                )
        }
)
public class RootConfig {
    @Value("${DB_DRIVER}") String driver;
    @Value("${DB_URL}") String url;
    @Value("${DB_USERNAME}") String username;
    @Value("${DB_PASSWORD}") String password;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ACCESS_EXPIRATION}") long accessExpiration,
            @Value("${JWT_REFRESH_EXPIRATION}") long refreshExpiration
    ) {
        return new JwtTokenProvider(secret, accessExpiration, refreshExpiration);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource);

        // ===== 이 줄이 빠져있었어요 =====
        sqlSessionFactory.setMapperLocations(
                applicationContext.getResources("classpath:/com/secondzip/mapper/**/*.xml")
        );

        return sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }
}