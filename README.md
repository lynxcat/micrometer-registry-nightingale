# micrometer-registry-nightingale  
micrometer数据上报到滴滴夜莺(nightingale)中  
  
## 使用方法  
使用方法，可以将micrometer-registry-nightingale-boot-starter,和micrometer-registry-nightingal安装到自己的maven仓库中，然后在项目中依赖micrometer-registry-nightingale-boot-starter即可  
   
## 参数配置   
```
management:   
  metrics:   
    export:  
      nightingale:  
        addr: agent地址  
        endpoint: 端点IP，如未配置则自动获得本机IP
        nid: 如果配置了nid，则视为机器无关指标，endpoint配置不再生效
        step: 上报频率  
        append-tags: 应用附加Tag, 格式"key1=value1,key2=value2"  
        enabled: 是否启用(true|false)  
``` 
  
## maven依赖
项目已经发布到了maven仓库中，在项目中添加依赖可以直接使用   
micrometer-rigistry-nightingale   
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-rigistry-nightingale</artifactId>
        <version>1.6.3</version>
    </dependency>
```
   
micrometer-rigistry-nightingale-boot-starter  
```
    <dependency>
        <groupId>com.github.lynxcat</groupId>
        <artifactId>micrometer-rigistry-nightingale-boot-starter</artifactId>
        <version>1.6.3</version>
    </dependency>
```
  
下版本将会支持通过agent提供的接口自动获取endpoint，用户可以将endpoint配置为ip地址
