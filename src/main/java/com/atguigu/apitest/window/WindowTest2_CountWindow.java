package com.atguigu.apitest.window;

import com.atguigu.apitest.beans.SensorReading;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author Demigod
 * @create 2020-11-10 20:06
 * @Desc
 */
public class WindowTest2_CountWindow {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 从文件读取数据
//        DataStream<String> inputStream = env.readTextFile("D:\\Projects\\BigData\\FlinkTutorial\\src\\main\\resources\\sensor.txt");

        // socket文本流
        DataStream<String> inputStream = env.socketTextStream("hadoop102", 7777);

        // 转换成SensorReading类型
        DataStream<SensorReading> dataStream = inputStream.map(new MapFunction<String, SensorReading>() {
            @Override
            public SensorReading map(String line) throws Exception {
                String[] fields = line.split(",");
                return new SensorReading(fields[0], new Long(fields[1]), new Double(fields[2]));
            }
        });
        SingleOutputStreamOperator<Double> avgTempResultStream = dataStream.keyBy("id")
                .countWindow(10, 2)
                .aggregate(new MyAvgTemp());
        avgTempResultStream.print("avg");
        env.execute();
    }

    private static class MyAvgTemp implements AggregateFunction<SensorReading, Tuple2<Double, Integer>, Double> {
        @Override
        public Tuple2<Double, Integer> createAccumulator() {
            return new Tuple2<Double, Integer>(0.0, 0);
        }

        @Override
        public Tuple2<Double, Integer> add(SensorReading sensorReading, Tuple2<Double, Integer> doubleIntegerTuple2) {
            return new Tuple2<Double, Integer>(sensorReading.getTemperature()+doubleIntegerTuple2.f0,doubleIntegerTuple2.f1+1);
        }

        @Override
        public Double getResult(Tuple2<Double, Integer> doubleIntegerTuple2) {
            return doubleIntegerTuple2.f0/doubleIntegerTuple2.f1;
        }

        @Override
        public Tuple2<Double, Integer> merge(Tuple2<Double, Integer> doubleIntegerTuple2, Tuple2<Double, Integer> acc1) {
            return new Tuple2<Double, Integer>(doubleIntegerTuple2.f0+acc1.f0,doubleIntegerTuple2.f1+acc1.f1);
        }
    }
}
