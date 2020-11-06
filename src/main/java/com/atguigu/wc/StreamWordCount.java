package com.atguigu.wc;/**
 * Copyright (c) 2018-2028 尚硅谷 All Rights Reserved
 * <p>
 * Project: FlinkTutorial
 * Package: com.atguigu.wc
 * Version: 1.0
 * <p>
 * Created by wushengran on 2020/11/6 11:48
 */

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @ClassName: StreamWordCount
 * @Description:
 * @Author: wushengran on 2020/11/6 11:48
 * @Version: 1.0
 */
public class StreamWordCount {
    public static void main(String[] args) throws Exception{
        // 创建流处理执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        env.setParallelism(1);

//        // 从文件中读取数据
//        String inputPath = "D:\\Projects\\BigData\\FlinkTutorial\\src\\main\\resources\\hello.txt";
//        DataStream<String> inputDataStream = env.readTextFile(inputPath);

        // 用parameter tool工具从程序启动参数中提取配置项
        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        String host = parameterTool.get("host");
        int port = parameterTool.getInt("port");

        // 从socket文本流读取数据
        DataStream<String> inputDataStream = env.socketTextStream(host, port);


        // 基于数据流进行转换计算
        DataStream<Tuple2<String, Integer>> resultStream = inputDataStream.flatMap(new WordCount.MyFlatMapper())
                .keyBy(0)
                .sum(1).setParallelism(2);

        resultStream.print().setParallelism(1);

        // 执行任务
        env.execute();
    }
}
