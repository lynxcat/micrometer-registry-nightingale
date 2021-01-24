# micrometer-registry-nightingale  
micrometer数据上报到滴滴夜莺(nightingale)中  
  
##使用方法  
使用方法，可以将micrometer-registry-nightingale-boot-starter,和micrometer-registry-nightingal安装到自己的maven仓库中，然后在项目中依赖micrometer-registry-nightingale-boot-starter即可  
   
##参数配置   
```
management:   
  metrics:   
    export:  
      nightingale:  
        addr: agent地址  
        endpoint: 端点IP  
        step: 上报频率  
        append-tags: 应用附加Tag, 格式"key1=value1,key2=value2"  
        enabled: 是否启用(true|false)  
``` 
  
预计下周一会更新一个版本，通过agent提供的接口自动获取endpoint 
