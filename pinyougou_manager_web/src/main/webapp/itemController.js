window.onload=function () {
    var app = new Vue({
        el:"#app",
        data:{
            //购买数量
            num:1,
            //记录用户选择的规格
            specificationItems: {},
            //当前将要购买的商品
            sku:{}
        },
        methods:{
            //增加入购物车的商品数量
            addNum:function (x) {
                this.num =Math.floor(x)+1;
                if(this.num < 1){
                    this.num = 1;
                }
            },
            //减加入购物车的商品数量
            subtractNum:function (x) {
                this.num =Math.floor(x)-1;
                if(this.num < 1){
                    this.num = 1;
                }
            },
            /**
             * 用户选择规格
             * @param specName 规格名称
             * @param optionName 选项名称
             */
            selectSpecification: function (specName, optionName) {
                //this.specificationItems[specName] = optionName;
                this.$set(this.specificationItems, specName, optionName);
                //更新sku
                this.searchSku();
            },
            /**
             * 判断某规格选项是否被用户选中
             * @param specName 规格名称
             * @param optionName 选项名称
             * @return {boolean}
             */
            isSelected: function (specName, optionName) {
                return this.specificationItems[specName] == optionName;
            },

            //加载默认要购买的商品
            loadSku:function(){
                //记录默认要购买的商品，由于后台跟据默认排序，所以这里第一条就是默认商品
                this.sku=skuList[0];
                //选中默认的规格，注意这里要用深克隆
                this.specificationItems= JSON.parse(JSON.stringify(this.sku.spec)) ;
            },

            //匹配两个对象的内容是否一致(tbGoodsDesc中的customAtributeItem和在tbItem中由goodsId查到的spec)
            matchObject:function(map1,map2){
                for(var k in map1){
                    //map1中的key在map2中能找到
                    if(map1[k]!=map2[k]){
                        return false;
                    }
                }
                for(var k in map2){
                    //map2中的key在map1中能找到
                    if(map2[k]!=map1[k]){
                        return false;
                    }
                }
                return true;
            },

            //选择规格后，查询相应的sku
            searchSku: function () {
                for (let i = 0; i < skuList.length; i++) {
                    //使用用户选中的规格信息与sku列表的规格信息对比
                    //根据选中的规格(由属性表显示在页面的),找到对应的sku
                    if (this.matchObject(skuList[i].spec, this.specificationItems)) {
                        this.sku = skuList[i];
                        return;
                    }
                }
                //如果没有匹配的
                this.sku = {id: 0, title: '--------', price: 0};
            },


            //添加购物车
            addToCart: function () {
                //先打印出来看看，后续课程使用到
                alert('skuid:' + this.sku.id);
            }


        },
        created:function () {
            this.loadSku();
        }
    });
}
