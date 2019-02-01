package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ManageController {

    @Reference
    private  ManageService manageService;

    @RequestMapping("index")
    public String index(){
        return  "index";
    }

    @RequestMapping("attrListPage")
    public String getAttrListPage(){
        return "attrListPage";
    }

    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){
       return manageService.getCatalog1();
    }
    @RequestMapping("getCatalog2")
    @ResponseBody
    public  List<BaseCatalog2> getCatalog2(String catalog1Id){

        return  manageService.getCatalog2(catalog1Id);
    }

    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return  manageService.getCatalog3(catalog2Id);
    }

    /*
        通过catalog3Id 查询平台属性BaseAttrInfo和平台属性值BaseAttrValue
        多表关联查询，不能使用通用mapper，需要使用Mybatis，手动写sql语句
        使用的表： base_attr_info  base_attr_value
            select * from base_attr_info bai  inner join  base_attr_value  bav on bai.id =bav.attr_id
            where bai.catalog3_id =?

           Mybatis{
               两个核心配置文件：
                mybatis-cfg.xml 核心配置文件
                    不推荐使用这种配置文件 ，可以用配置文件application.properties中设置
                        mybatis.mapper-locations=classpath:mapper/*Mapper.xml
                        mybatis.configuration.mapUnderscoreToCamelCase=true

                    <mappers>
                        <mapper  package="com.atguigu.mapper">

                        </mapper>
                    </mappers>
            package com.atguigu.mapper
                    UserMapper.java
                UserMapper.xml  映射实体类的配置文件


           }
     */
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){

        return  manageService.getAttrList(catalog3Id);
    }


    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public  String saveAttrInfo(BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }


    @RequestMapping("deleteAttrInfo")
    @ResponseBody
    public String deleteAttrInfoByPremaryKey(String id){
        manageService.deleteAttrInfoByPremaryKey(id);
        return "success";
    }




}
