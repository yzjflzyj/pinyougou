window.onload=function () {
    var app = new Vue({
        el:"#app",
        data:{
            //购物车列表
            cartList:[],
            //统计数据
            totalValue:{totalNum:0,totalMoney:0.0},
            //收件人列表
            addressList:[],
            //用户当前选中的地址
            address:{},
            //订单对象{paymentType:支付方式(默认是1)}
            order:{paymentType:1}
        },
        methods:{
            //查询原有购物车列表
            findCartList:function () {
                axios.get("/cart/findCartList.do").then(function (response) {
                    app.cartList = response.data;
                    //每次查询后重新计算金额与数量
                    app.totalValue={totalNum:0, totalMoney:0.00 };
                    //统计数量与金额
                    for (let i = 0; i < app.cartList.length; i++) {
                        let cart = app.cartList[i];
                        for (let j = 0; j < cart.orderItemList.length; j++) {
                            //读取购物车明细，统计数量与金额
                            let orderItem = cart.orderItemList[j];
                            app.totalValue.totalNum += orderItem.num;
                            app.totalValue.totalMoney += orderItem.totalFee;
                        }
                    }

                })
            },

            /**
             * //修改购物车
             * @param itemId skuId
             * @param num 操作数量
             */
            addGoodsToCartList: function (itemId, num) {
                axios.get("/cart/addGoodsToCartList.do?itemId=" + itemId + "&num=" + num).then(function (response) {
                    if(response.data.success){
                        //刷新购物车列表
                        app.findCartList();
                    }else{
                        alert(response.data.message);
                    }
                });
            },

            //查询收件人列表
            findAddressList:function () {
                axios.get("/address/findListByLoginUser.do").then(function (response) {
                    app.addressList = response.data;
                    //设置默认地址读取
                    for (let i = 0; i < app.addressList.length; i++) {
                        if (app.addressList[i].isDefault == '1') {
                            app.address = app.addressList[i];
                            break;
                        }
                    }

                })
            },

            /**
             * 用户选择收件人
             * @param 收件人信息
             */
            selectAddress:function(address){
                this.address = address;
            },

            //提交保存订单
            submitOrder: function () {
                this.order.receiverAreaName = this.address.address;//地址
                this.order.receiverMobile = this.address.mobile;//手机
                this.order.receiver = this.address.contact;//联系人
                //提交订单
                axios.post("/order/add.do",this.order).then(function (response) {
                    if (response.data.success) {
                        //页面跳转
                        if (app.order.paymentType == '1') {//如果是微信支付，跳转到支付页面
                            location.href = "pay.html";
                        } else {//如果货到付款，跳转到提示页面
                            location.href = "paysuccess.html";
                        }
                    } else {
                        alert(response.data.message);   //也可以跳转到提示页面
                    }
                });
            }


        },
        created:function () {
            this.findCartList();
            this.findAddressList();
        }
    });
}
