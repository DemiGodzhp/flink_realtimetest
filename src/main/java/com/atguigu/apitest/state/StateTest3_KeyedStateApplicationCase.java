package com.atguigu.apitest.state;

import com.atguigu.apitest.beans.SensorReading;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * @author Demigod
 * @create 2020-11-10 20:42
 * @Desc
 */
public class StateTest3_KeyedStateApplicationCase {
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

        // 定义一个flatmap操作，检测温度跳变，输出报警
        SingleOutputStreamOperator<Tuple3<String, Double, Double>> resultStream = dataStream.keyBy("id")
                .flatMap(new TempChangeWarning(10.0));

        resultStream.print();

        env.execute();
    }


    private static class TempChangeWarning extends RichFlatMapFunction<SensorReading, Tuple3<String, Double, Double>> {
        // 私有属性，温度跳变阈值
        private Double threshold;

        // 定义状态，保存上一次的温度值
        private ValueState<Double> lastTempState;

        private TempChangeWarning(Double threshold) {
            this.threshold = threshold;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            lastTempState = getRuntimeContext().getState(new ValueStateDescriptor<Double>("last-temp", Double.class));
        }

        @Override
        public void close() throws Exception {
            lastTempState.clear();
        }

        @Override
        public void flatMap(SensorReading sensorReading, Collector<Tuple3<String, Double, Double>> collector) throws Exception {
            // 获取状态
            Double lastTemp = lastTempState.value();

            // 如果状态不为null，那么就判断两次温度差值
            if( lastTemp != null ){
                Double diff = Math.abs( sensorReading.getTemperature() - lastTemp );
                if( diff >= threshold )
                    collector.collect(new Tuple3<String, Double, Double>(sensorReading.getId(), lastTemp, sensorReading.getTemperature()));
            }

            // 更新状态
            lastTempState.update(sensorReading.getTemperature());
        }
    }
}
