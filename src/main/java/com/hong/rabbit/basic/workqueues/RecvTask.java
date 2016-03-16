package com.hong.rabbit.basic.workqueues;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by caihongwei on 16/3/15 下午8:45.
 */
public class RecvTask {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for message. To exit press CTRL+C");

        // 优点:将消息发送给未工作的worker;
        // 缺点:如果workers都在忙,则放在queue里,直到queue满;
        channel.basicQos(1);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");

                try {
                    doWork(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(" [x] Done");
                    // 手动回复消息处理,当另一台worker down了时,此worker会自动处理; 可防止消息丢失
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        channel.basicConsume(QUEUE_NAME, false, consumer);  // 上面设置了手动回复消息处理,这里关闭自动回复
    }

    private static void doWork(String task) throws InterruptedException {
        for (char c : task.toCharArray()) {
            if (c == '.')
                Thread.sleep(100);
        }
    }
}
