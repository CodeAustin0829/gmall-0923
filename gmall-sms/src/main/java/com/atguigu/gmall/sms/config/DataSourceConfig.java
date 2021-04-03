package com.atguigu.gmall.sms.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源配置
 *
 * @author HelloWoodes
 */
@Configuration
public class DataSourceConfig {

//    @Bean
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public HikariDataSource hikariDataSource(@Value("${spring.datasource.url}")String url) {
//        /**
//         * 因为 HikariDataSource 跟 DruidDataSource 数据源的名称不一致
//         * DruidDataSource：url
//         * HikariDataSource：JdbcUrl
//         * 所以需要手动设置一下，否则读取配置文件无法映射上
//         * 参数 @Value("${spring.datasource.url}")String url
//         * 表示将配置文件的值读取出来，赋值给临时变量url
//         */
//        HikariDataSource hikariDataSource = new HikariDataSource();
//        hikariDataSource.setJdbcUrl(url);
//        return hikariDataSource;
//    }

    /**
     * 需要将 DataSourceProxy 设置为主数据源，否则事务无法回滚
     *
     * @return The default datasource
     */
    @Primary
    @Bean("dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.username}") String userName,
            @Value("${spring.datasource.password}") String passWord,
            @Value("${spring.datasource.url}") String url
    ) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(driverClassName);
        hikariDataSource.setUsername(userName);
        hikariDataSource.setPassword(passWord);
        hikariDataSource.setJdbcUrl(url);
        return new DataSourceProxy(hikariDataSource);
    }
}
