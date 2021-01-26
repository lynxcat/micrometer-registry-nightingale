# micrometer-registry-nightingale  
micrometer数据上报到滴滴夜莺(nightingale)中  
  
## 使用方法  
1. 如果是spring boot项目，引入micrometer-registry-nightingale-boot-starter依赖  
2. 进行必要的参数配置，如果agent和jvm是在同一台机器的，只需要配置 management.metrics.export.nightingale.enabled=true，即可自动上报，其余配置信息请参考参数配置  

   
## 参数配置   
```
management:   
  metrics:   
    export:  
      nightingale:  
        addr: agent地址  
        endpoint: http://127.0.0.1:2080/endpoint 端点IP，如未配置则自动获得本机IP，如果配置的是URL则自动获取URL的值，启动时获取，后续不会自动刷新
        nid: 如果配置了nid，则视为机器无关指标，endpoint配置不再生效
        step: 上报频率，默认配置10S，格式为java.time.Duration
        append-tags: 应用附加Tag, 格式"key1=value1,key2=value2"  
        enabled: 是否启用(true|false)  
        metric-block-list: #需要屏蔽的metric
``` 
  
## maven依赖
项目已经发布到了maven仓库中，在项目中添加依赖可以直接使用   
micrometer-registry-nightingale   
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-registry-nightingale</artifactId>
        <version>1.6.3</version>
    </dependency>
```
   
micrometer-registry-nightingale-boot-starter  
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-registry-nightingale-boot-starter</artifactId>
        <version>1.6.3</version>
    </dependency>
```

## 实战入门  
1.建立一个spring boot项目，版本最好是2.3.5  
2.pom引入必要依赖  
  ```
    #pom.xml
    ....
    <dependencies>
        <!-- Spring boot web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.3.5.RELEASE</version>
        </dependency>

        <!-- Spring boot 监控 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
            <version>2.3.5.RELEASE</version>
        </dependency>

        <!-- lombok不是必须的，但建议引用 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.16</version>
        </dependency>

        <!-- 上报到滴滴的唯一包 -->
        <dependency>
            <groupId>com.github.lynxcat</groupId>
            <artifactId>micrometer-registry-nightingale-boot-starter</artifactId>
            <version>1.6.3</version>
        </dependency>
    </dependencies>
    ....
  ```
3.建立application.yml，进行必须配置
```
    #application.yml
    .....
    management:
      metrics:
        export:
          nightingale:
            addr: "http://127.0.0.1:2080/v1/push" #改成你自己的agent的ip和prot
            endpoint: "192.168.1.1"  #改成你自己的endpoint，如果不知道先登录夜莺看一下
            step: 10s    #采集时间
            append-tags: "key=value"  #附加的tags
            enabled: true #是否开启，这个参数一定要配置，不然不会加载插件
      endpoints:
        web:
          exposure:
            include: "*"   #标识开启所有的spring boot监控端点，详细请参照actuator项目
    logging:
      level:
        com.lynxcat: debug   #配置日志，可以看到上报日志信息
    ....
```
4.建立主启动类，启动项目即可到夜莺中查看相关数据

5.如果按照上面四步不成功的，可以试试看用项目下的 micrometer-registry-nightingale-boot-starter-test 项目，如果还有问题就提issue吧，看到了会处理的

  

## 其他说明  

默认上报所有的 metrics，不需要的数据通过actuator配置去掉对应端点即可。支持自定义metrics上报，项目底层依赖micrometer-core，自定义metrics请查看micrometer项目官网  
maven中央仓库中的版本暂时不支持metric-block-list和通过url地址获取endpoint。后续如有>可能会将这个项目提交给micrometer，因此，项目版本与micrometer-core的版本保持一致，新增的这两个功能预计在1.6.4版本进行发布，时间要看micrometer-core什么时候发布了。如需要这两个功能，请直接使用源码

夜莺只支持COUNTER和GAUGE类型的metrics，对于非这两种类型的metrics全部转换为GAUGE数据上报，数据格式的转换参照了micrometer-registry-elastic项目，不过这个项目官方也没有具体的文档
转换格式大致如下，详细的请看源码或者看日志输出：  
```
#jvm.gc.pause 在actuator
{
  "name": "jvm.gc.pause",
  "description": "Time spent in GC pause",
  "baseUnit": "milliseconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 0
    },
    {
      "statistic": "MAX",
      "value": 0
    }
  ]
}

#上到夜莺

[{"timestamp":1611545811,"metric":"jvm.gc.pause","counterType":"GAUGE","step":10,"endpoint":"192.168.230.131","tags":"action=end-of-minor-GC,cause=Allocation-Failure","value":0},
{"timestamp":1611545811,"metric":"jvm.gc.pause.sum","counterType":"GAUGE","step":10,"endpoint":"192.168.230.131","tags":"action=end-of-minor-GC,cause=Allocation-Failure","value":0.0},
{"timestamp":1611545811,"metric":"jvm.gc.pause.mean","counterType":"GAUGE","step":10,"endpoint":"192.168.230.131","tags":"action=end-of-minor-GC,cause=Allocation-Failure","value":0.0},
{"timestamp":1611545811,"metric":"jvm.gc.pause.max","counterType":"GAUGE","step":10,"endpoint":"192.168.230.131","tags":"action=end-of-minor-GC,cause=Allocation-Failure","value":0.0}]

#可以通过以下配置来查看上报的数据结构
logging.level=com.lynxcat: debug
```
