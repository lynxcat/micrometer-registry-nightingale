# micrometer-registry-nightingale  
micrometer数据上报到滴滴夜莺(nightingale)中  
  
## 使用方法  
1. 如果是spring boot项目，引入micrometer-rigistry-nightingale-boot-starter依赖  
2. 进行必要的参数配置，如果agent和jvm是在同一台机器的，只需要配置 management.metrics.export.nightingale.enabled=true，即可自动上报，其余配置信息请参考参数配置  

   
## 参数配置   
```
management:   
  metrics:   
    export:  
      nightingale:  
        addr: agent地址  
        endpoint: 端点IP，如未配置则自动获得本机IP
        nid: 如果配置了nid，则视为机器无关指标，endpoint配置不再生效
        step: 上报频率，默认配置10S，格式为java.time.Duration
        append-tags: 应用附加Tag, 格式"key1=value1,key2=value2"  
        enabled: 是否启用(true|false)  
``` 
  
## maven依赖
项目已经发布到了maven仓库中，在项目中添加依赖可以直接使用   
micrometer-rigistry-nightingale   
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-registry-nightingale</artifactId>
        <version>1.6.3</version>
    </dependency>
```
   
micrometer-rigistry-nightingale-boot-starter  
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-registry-nightingale-boot-starter</artifactId>
        <version>1.6.3</version>
    </dependency>
```

## 其他说明  
默认上报所有的 metrics，如果不想上报的可以通过actuator配置去掉。夜莺只支持COUNTER和GAUGE类型的metrics，对于非这两种类型的metrics全部转换为GAUGE数据上报  
转换格式如下：
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
  
下版本将会支持通过agent提供的接口自动获取endpoint，用户可以将endpoint配置为ip地址
