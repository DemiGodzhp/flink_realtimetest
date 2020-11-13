package com.atguigu.processfuntion;

import com.atguigu.apitest.beans.SensorReading;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

/**
 * @author Demigod
 * @create 2020-11-12 14:44
 * @Desc
 */
public class ProcessTest1_KeyedProcessFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // socket文本流
        DataStream<String> inputStream = env.socketTextStream("localhost", 7777);

        // 转换成SensorReading类型
        DataStream<SensorReading> dataStream = inputStream.map(new MapFunction<String, SensorReading>() {
            @Override
            public SensorReading map(String line) throws Exception {
                String[] fields = line.split(",");
                return new SensorReading(fields[0], new Long(fields[1]), new Double(fields[2]));
            }
        });

        // 测试KeyedProcessFunction，先分组然后自定义处理
        dataStream.keyBy("id")
                .process( new MyProcess() )
                .print();

        env.execute();

    }

    private static class MyProcess extends KeyedProcessFunction<Tuple, SensorReading, Integer> {
        ValueState<Long> tsTimerState;

        @Override
        public void open(Configuration parameters) throws Exception {
            //能够获取到当前的状态
            tsTimerState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("ts-timer", Long.class));
        }

        @Override
        public void close() throws Exception {
            tsTimerState.clear();
        }

        @Override
        public void processElement(SensorReading sensorReading, Context context, Collector<Integer> collector) throws Exception {
            collector.collect(sensorReading.getId().length());

            // context
            context.timestamp();//当前的时间戳
            context.getCurrentKey();//当前的key
//            ctx.output();
            context.timerService().currentProcessingTime();
            context.timerService().currentWatermark();
            context.timerService().registerProcessingTimeTimer( context.timerService().currentProcessingTime() + 5000L);
            tsTimerState.update(context.timerService().currentProcessingTime() + 1000L);

            context.timerService().registerEventTimeTimer((sensorReading.getTimestamp() + 10) * 1000L);
//          ctx.timerService().deleteProcessingTimeTimer(tsTimerState.value());

        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<Integer> out) throws Exception {
            System.out.println(timestamp + " 定时器触发");
            ctx.getCurrentKey();
//            ctx.output();
            ctx.timeDomain();
        }
    }


}
