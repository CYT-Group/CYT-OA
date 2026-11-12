package com.cyt.center;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 个人中心后端启动类。
 * 根包下只有 core（全局技术）和 module（业务域），不再有 framework / starter 子工程。
 */
@EnableScheduling
@SpringBootApplication
// 自建模块的 mapper 在 *.mapper，从芋道移植过来的沿用 *.dal.postgresql，两边都要扫到
@MapperScan({"com.cyt.center.module.**.mapper", "com.cyt.center.module.**.dal.postgresql"})
public class CenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CenterApplication.class, args);
    }

}
