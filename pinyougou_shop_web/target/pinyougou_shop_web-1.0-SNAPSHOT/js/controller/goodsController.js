//页面初始化完成后再创建Vue对象
//window.onload=function () {
//创建Vue对象
var app = new Vue({
    //接管id为app的区域
    el: "#app",
    data: {
        //声明数据列表变量，供v-for使用
        list: [],
        //总页数
        pages: 1,
        //当前页
        pageNo: 1,
        //声明对象
        entity: {
            goods: {typeTemplateId: 0},
            goodsDesc: {itemImages: [], customAttributeItems: [], specificationItems: []},
            itemList: []
        },
        //entity.itemList:[{spec:{},price:0,num:99999,status:'0',isDefault:'0' }]
        //将要删除的id列表
        ids: [],
        //搜索包装对象
        searchEntity: {},
        //图片上传成功后保存的对象
        image_entity: {url: ''},
        //一级分类
        itemCatList1: [],
        //二级分类
        itemCatList2: [],
        //三级分类
        itemCatList3: [],
        //品牌列表
        brandIds: [],
        //规格列表(由模板决定的所有复选框)
        specIds: [],
        //商品状态
        status:['未审核','已审核','审核未通过','关闭'],
        //分类集合
        itemCatMap:{"1":"手机"}



    },
    methods: {
        //查询所有
        findAll: function () {
            axios.get("../goods/findAll.do").then(function (response) {
                //vue把数据列表包装在data属性中
                app.list = response.data;
            }).catch(function (err) {
                console.log(err);
            });
        },
        //分页查询
        findPage: function (pageNo) {
            axios.post("../goods/findPage.do?pageNo=" + pageNo + "&pageSize=" + 10, this.searchEntity)
                .then(function (response) {
                    app.pages = response.data.pages;  //总页数
                    app.list = response.data.rows;  //数据列表
                    app.pageNo = pageNo;  //更新当前页
                });
        },
        //让分页插件跳转到指定页
        goPage: function (page) {
            app.$children[0].goPage(page);
        },
        //新增
        add: function () {
            var url = "../goods/add.do";
            if (this.entity.id != null) {
                url = "../goods/update.do";
            }
            //获取富文本里的内容
            this.entity.goodsDesc.introduction = editor.html();
            axios.post(url, this.entity).then(function (response) {
                if (response.data.success) {
                  /*  //清空富文本内容
                    editor.html(" ");
                    //刷新数据，刷新当前页
                    app.entity = {goods: {}, goodsDesc: {}};*/
                    window.location.href = "goods.htm";
                } else {
                    //失败时显示失败消息
                    alert(response.data.message);
                }
            });
        },
        //跟据id查询
        getById: function () {
            //获取url上的id
            let id = this.getUrlParam()["id"];
            if (id!=null) {
                axios.get("../goods/getById.do?id=" + id).then(function (response) {
                    app.entity = response.data;
                    //绑定富文本内容
                    editor.html(app.entity.goodsDesc.introduction);
                    //把图片json字符串转换成对象
                    app.entity.goodsDesc.itemImages = JSON.parse(app.entity.goodsDesc.itemImages);
                    //把扩展属性json字符串转换成对象
                    app.entity.goodsDesc.customAttributeItems = JSON.parse(app.entity.goodsDesc.customAttributeItems);
                    //把规格json字符串转换成对象
                    app.entity.goodsDesc.specificationItems = JSON.parse(app.entity.goodsDesc.specificationItems);
                    //SKU列表规格列转换  json串转换为数组以显示
                    for( let i = 0; i < app.entity.itemList.length; i++){
                        app.entity.itemList[i].spec = JSON.parse(app.entity.itemList[i].spec);
                    }

                });
            }
        },

        /**
         * 识别规格的checkbox要不要选中
         * @param specName 规格名称
         * @param optionName 选项名称
         * @return true|false
         */
        checkAttributeValue:function(specName,optionName){
            let items = app.entity.goodsDesc.specificationItems;
            for(let i = 0; i < items.length; i++){
                let obj = this.searchObjectByKey(items, "attributeName", specName);
                if(obj != null){
                    //如果匹配到相应的数据
                    if(obj.attributeValue.indexOf(optionName) > -1){
                        return true;
                    }
                }
            }
            return false;
        },

        //批量删除数据
        dele: function () {
            axios.get("../goods/delete.do?ids=" + this.ids).then(function (response) {
                if (response.data.success) {
                    //刷新数据
                    app.findPage(app.pageNo);
                    //清空勾选的ids
                    app.ids = [];
                } else {
                    alert(response.data.message);
                }
            })
        },
        //文件上传
        upload: function () {
            // 声明一个FormData对象
            var formData = new FormData();
            // 'file' 这个名字要和后台获取文件的名字一样;
            formData.append('file', document.querySelector('input[type=file]').files[0]);
            //post提交
            axios({
                url: '/upload.do',
                data: formData,
                method: 'post',
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            }).then(function (response) {
                if (response.data.success) {
                    //上传成功
                    app.image_entity.url = response.data.message;
                } else {
                    //上传失败
                    alert(response.data.message);
                }
            })
        },
        //保存图片列表
        add_image_entity: function () {
            this.entity.goodsDesc.itemImages.push(this.image_entity);
        },
        //删除图片
        remove_image_entity: function (index) {
            this.entity.goodsDesc.itemImages.splice(index, 1);
        },
        //根据父id查询子类列表
        findItemCatList: function (parentId, grade) {
            axios.get("/itemCat/findByParentId.do?parentId=" + parentId).then(function (response) {
                //app["itemCat"+grade]相当于app.itemCat1至3
                app["itemCatList" + grade] = response.data;
            })

        },
        /**
         * 判断数组中元素对象是否已经存在某个属性对应的的属性值
         * (复选框选中的规格数组中,某一规格名是否存在)
         * @param list
         * @param key
         * @param keyValue
         * @returns {*}
         */
        searchObjectByKey: function (list, key, keyValue) {
            for (var i = 0; i < list.length; i++) {
                //如果找到相应属性的值，返回找到的对象
                if (list[i][key] == keyValue) {
                    return list[i];
                }
            }
            //找不到返回空
            return null;
        },

        //要从复选框中保存信息,需要参数,复选框,规格名和规格选项名,最终拿到规格名数组specificationItems
        updateSpecAttribute: function (event, specName, optionName) {

            var obj = this.searchObjectByKey(this.entity.goodsDesc.specificationItems, "attributeName", specName);
            if (event.target.checked) {
                if (obj == null) {
                    //判断该规格名是否已经被选过了,没有被选过则在数组中添加该规格名的数组元素
                    this.entity.goodsDesc.specificationItems.push({
                        "attributeName": specName,
                        "attributeValue": [optionName]
                    });
                } else {
                    //已经选择过,则往该规格名属性中继续添加规格选项
                    obj.attributeValue.push(optionName);
                }

            } else {
                //查找当前optionName的下标,删除规格选项元素
                var optionIndex = obj.attributeValue.indexOf(optionName);
                obj.attributeValue.splice(optionIndex, 1);
                //判断该规格名是否是该规格名的最后一个规格选项,如果是则删除该规格名的数组元素
                //判断该规格名是否是该规格名的最后一个规格选项,如果不是则删除该规格名的该规格选项
                if (obj.attributeValue.length == 0) {
                    var specIndex = this.entity.goodsDesc.specificationItems.indexOf(obj);
                    this.entity.goodsDesc.specificationItems.splice(specIndex, 1);
                }
            }

            /*  //判断该规格名是否存在过
              var obj = this.searchObjectByKey(this.entity.goodsDesc.specificationItems, 'attributeName', specName);
              if (obj == null) {
                  //不存在则添加该规格名及其规格选项(不存在,则点击复选框,一定是选中)
                  this.entity.goodsDesc.specificationItems.push({
                      "attributeName": specName,
                      "attributeValue": [
                          optionName
                      ]
                  });
              } else {
                  //存在该规格名,则再判断是选中复选框还是取消操作
                  if (event.target.checked) {
                      // 选中操作,追加规格选项元素
                      obj.attributeValue.push(optionName);
                  } else {
                      //取消操作,则删除其规格选项
                      var optionIndex = obj.attributeValue.indexOf(optionName);
                      obj.attributeValue.splice(optionIndex, 1);
                      //如果取消勾选后，选项列表已经没有了,则移除整个规格名称列表
                      if (obj.attributeValue.length <1) {
                          var specIndex = this.entity.goodsDesc.spec
                          .0ificationItems.indexOf(obj);
                          this.entity.goodsDesc.specificationItems.splice(specIndex, 1);
                      }

                  }
              }*/
        },
        // 1.创建createItemList方法，同时创建一条有基本数据，不带规格的初始数据
        createItemList: function () {
            // 参考: this.entity.itemList=[{spec:{},price:0,num:99999,status:'0',isDefault:'0' }]
            this.entity.itemList = [{spec: {}, price: 0, num: 99999, status: '0', isDefault: '0'}];
            // 2.查找遍历所有已选择的规格列表，后续会重复使用它，所以我们可以抽取出个变量items
            var items = this.entity.goodsDesc.specificationItems;
            for (var i = 0; i < items.length; i++) {
                // 9.回到createItemList方法中，在循环中调用addColumn方法，并让itemList重新指向返回结果;
                this.entity.itemList = this.addColumn(this.entity.itemList, items[i].attributeName, items[i].attributeValue);
            }
        },
         // 3.抽取addColumn(当前的表格，列名称，列的值列表)方法，用于每次循环时追加列
        addColumn: function (list, specName, optionValue) {
            // 4.编写addColumn逻辑，当前方法要返回添加所有列后的表格，定义新表格变量newList
            var newList = [];
            // 5.在addColumn添加两重嵌套循环，一重遍历之前表格的列表，二重遍历新列值列表
            for (var i = 0; i < list.length; i++) {
                for (var j = 0; j < optionValue.length; j++) {
                    // 6.在第二重循环中，使用深克隆技巧，把之前表格的一行记录copy所有属性，
                    // 用到var newRow = JSON.parse(JSON.stringify(之前表格的一行记录));
                    var newRow = JSON.parse(JSON.stringify(list[i]));
                    // 7.接着第6步，向newRow里追加一列
                    newRow.spec[specName] = optionValue[j];
                    // 8.把新生成的行记录，push到newList中
                    newList.push(newRow);
                }
            }
            return newList;
        },
        //将所有分类id及对应分类名查出,装在map集合itemCatMap中
        findAllItemCat:function () {
            axios.get("/itemCat/findAll.do").then(function (response) {
                for (let i=0;i<response.data.length;i++) {
                app.$set(app.itemCatMap,response.data[i].id,response.data[i].name);
                }
            })
        },

        /**
         * 解析一个url中所有的参数
         * @return {参数名:参数值}
         */
        getUrlParam:function() {
            //url上的所有参数
            var paramMap = {};
            //获取当前页面的url
            var url = document.location.toString();
            //获取问号后面的参数
            var arrObj = url.split("?");
            //如果有参数
            if (arrObj.length > 1) {
                //解析问号后的参数
                var arrParam = arrObj[1].split("&");
                //读取到的每一个参数,解析成数组
                var arr;
                for (var i = 0; i < arrParam.length; i++) {
                    //以等于号解析参数：[0]是参数名，[1]是参数值
                    arr = arrParam[i].split("=");
                    if (arr != null) {
                        paramMap[arr[0]] = arr[1];
                    }
                }
            }
            return paramMap;
        }




    },
    //监听变量的变化触发某些逻辑
    watch: {
        //当一级分类变量变化后，触发以下逻辑
        //参数(改后新的值，改前旧的值)
        "entity.goods.category1Id": function (newValue, oldValue) {
                //查询二级分类
                this.findItemCatList(newValue, 2);
                //三级分类清空
                this.itemCatList3 = [];
                //模板id重置
                this.entity.goods.typeTemplateId = 0;

        },
        //当二级分类变量变化后，触发以下逻辑
        "entity.goods.category2Id": function (newValue, oldValue) {
            //查询三级分类
            this.findItemCatList(newValue, 3);
            //模板id重置
            this.entity.goods.typeTemplateId = 0;
        },
        //当三级分类变量变化后，查询模板Id
        "entity.goods.category3Id": function (newValue, oldValue) {
                //查询模板Id
                axios.get("/itemCat/getById.do?id=" + newValue).then(function (response) {

                    app.entity.goods.typeTemplateId = response.data.typeId;

                });
        },
        //当模板id变量变化后，查询模板信息
        "entity.goods.typeTemplateId": function (newValue, oldValue) {
                //查询模板信息(根据模板id查到模板对象,再查到品牌和属性)
                axios.get("/typeTemplate/getById.do?id=" + newValue).then(function (response) {
                    //品牌列表
                    app.brandIds = JSON.parse(response.data.brandIds);
                    //扩展属性列表
                    //获取url参数,在回显的时候不让其生效,由查出来的图片显示
                    let id = app.getUrlParam()["id"];
                    if(id == null) {
                        app.entity.goodsDesc.customAttributeItems = JSON.parse(response.data.customAttributeItems);
                    }
                    //拿到规格(包含其规格选项)的List<Map>
                    axios.get("/typeTemplate/findSpecList.do?id=" + newValue).then(function (response) {
                        app.specIds = response.data;
                    })
                })
        },



    },
    //Vue对象初始化后，调用此逻辑
    created: function () {
        //调用用分页查询，初始化时从第1页开始查询
        this.findPage(1);
        //查询商品一级分类
        this.findItemCatList(0, 1);
        //查询所有分类
        this.findAllItemCat();
        //跟据id查询商品信息
        this.getById();


    }

});
//}
