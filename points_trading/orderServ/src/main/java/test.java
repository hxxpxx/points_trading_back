import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: points_trading
 * @BelongsPackage: PACKAGE_NAME
 * @Author: lizongle
 * @CreateTime: 2022-07-21  09:05
 * @Description:
 * @Version: 1.0
 */
public class test {
    public static void main(String[] args) throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        // 1.创建消息生产者producer，并指定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("group1");
        // 2.指定Nameserver地址
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.setSendMsgTimeout(15000);
        // 3.启动producer
        producer.start();
        for (int i = 0; i < 10; i++) {
            // 4.创建消息对象，指定主题Topic、Tag和消息体
            /**
             * 参数说明：
             * 参数一：消息主题Topic
             * 参数二：消息Tag
             * 参数三：消息内容
             */
            Message msg = new Message("topic1", "tag1", ("Hello RocketMQ"+i).getBytes());
            // 5.发送消息
            SendResult result = producer.send(msg);
            System.out.println("结果信息：" + result);
            TimeUnit.SECONDS.sleep(1); // 线程睡1秒
        }

        // 6.关闭生产者producer
        producer.shutdown();
    }
}
