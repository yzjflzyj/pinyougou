package com.itheima;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class MqTest {

    public static void main(String[] args) throws Exception{
        //1.创建生产者-producer = new DefaultMQProducer(组名)
        DefaultMQProducer producer = new DefaultMQProducer("producerGroup");
        //2.设置NameServer地址-producer.setNamesrvAddr(),如果集群环境，则用分号分隔
        producer.setNamesrvAddr("127.0.0.1:9876");
        //3.启动生产者-producer.start()
        producer.start();
        //4.创建消息-message = new Message(主题名,标签名,消息key名,消息内容.getBytes(RemotingHelper.DEFAULT_CHARSET));
        Message message = new Message(
                "topic-1",
                "tag-1",
                "key-1",
                "这是我第1次发送MQ消息".getBytes(RemotingHelper.DEFAULT_CHARSET)
        );
        //5.发送消息，接收结果-sendResult = producer.send(message)
        SendResult sendResult = producer.send(message);
        //6.输出sendResult查看消息是否成功送达
        System.out.println(sendResult);
        //7.如果不再发送消息，关闭生产者-producer.shutdown
        producer.shutdown();
    }

}
