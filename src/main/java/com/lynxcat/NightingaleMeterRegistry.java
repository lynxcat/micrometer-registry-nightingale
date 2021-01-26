package com.lynxcat;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.MeterPartition;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.ipc.http.HttpSender;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.core.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.util.StringEscapeUtils.escapeJson;
import static java.util.stream.Collectors.joining;


public class NightingaleMeterRegistry extends StepMeterRegistry {

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("ngihtingale-metrics-publisher");
    private static final String SUCCESS_RESPONSE_BODY_SIGNATURE = "{\"err\":\"\"}";
    private final NightingaleConfig config;
    private final HttpSender httpClient;
    private final Logger logger = LoggerFactory.getLogger(NightingaleMeterRegistry.class);
    private String endpoint;

    public NightingaleMeterRegistry(NightingaleConfig config, Clock clock) throws Throwable {
        this(config, clock, DEFAULT_THREAD_FACTORY,
                new HttpUrlConnectionSender(config.connectTimeout(), config.readTimeout()));
    }

    public NightingaleMeterRegistry(NightingaleConfig config, Clock clock, ThreadFactory threadFactory, HttpSender httpClient) throws Throwable {
        super(config, clock);
        this.config = config;
        this.httpClient = httpClient;
        config().namingConvention(NamingConvention.dot);

        try {
            //如果endpoint配置的是URI格式的字符串，发送一个http请求获得endpoint
            URI.create(config.endpoint()).toURL();
            httpClient.get(config.endpoint()).send().onSuccess(response -> {
                endpoint = response.body();
            }).onError(response -> {
                endpoint = config.endpoint();
            });
        } catch (MalformedURLException e){
            endpoint = config.endpoint();
        } catch (IllegalArgumentException e){
            endpoint = config.endpoint();
        }
        start(threadFactory);
    }



    @Override
    protected void publish() {
        for (List<Meter> batch : MeterPartition.partition(this, config.batchSize())) {
            try {
                String requestBody = batch.stream()
                        .map(m -> m.match(
                                this::writeGauge,
                                this::writeCounter,
                                this::writeTimer,
                                this::writeSummary,
                                this::writeLongTaskTimer,
                                this::writeTimeGauge,
                                this::writeFunctionCounter,
                                this::writeFunctionTimer,
                                this::writeMeter))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(joining(",\n", "[", "]"));

                logger.debug(requestBody);

                httpClient
                        .post(config.addr())
                        .withJsonContent(requestBody)
                        .send()
                        .onSuccess(response -> {
                            String responseBody = response.body();
                            if (responseBody.contains(SUCCESS_RESPONSE_BODY_SIGNATURE)){
                                logger.debug("successfully sent {} metrics to nightingale", responseBody);
                            }else {
                                logger.debug("failed metrics payload: {}", requestBody);
                                logger.error("failed to send metrics to nightingale: {}", responseBody);
                            }
                        })
                        .onError(response -> {
                            logger.debug("failed metrics payload: {}", requestBody);
                            logger.error("failed to send metrics to nightingale: {}", response.body());
                        });

            } catch (Throwable e) {
                e.printStackTrace();
                logger.error("failed to send metrics to nightingale: {}", e.getMessage());
            }
        }
    }

    protected Long generateTimestamp() {
        return config().clock().wallTime() / 1000;
    }


    public Consumer<StringBuilder> getConsumer(StringBuilder sb){
        return (builder) -> {
            builder.append(",\"value\":").append(sb);
        };
    }

    // VisibleForTesting
    Optional<String> writeCounter(Counter counter) {
        return writeCounter(counter, counter.count());
    }

    // VisibleForTesting
    Optional<String> writeFunctionCounter(FunctionCounter counter) {
        return writeCounter(counter, counter.count());
    }

    // VisibleForTesting
    private Optional<String> writeCounter(Meter meter, double value) {
        if (Double.isFinite(value)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(writeDocument(meter, getConsumer(new StringBuilder().append(value))));
            return writeReturn(list);
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeGauge(Gauge gauge) {
        double value = gauge.value();
        if (Double.isFinite(value)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(writeDocument(gauge, getConsumer(new StringBuilder().append(value))));
            return writeReturn(list);
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeTimeGauge(TimeGauge gauge) {
        double value = gauge.value(getBaseTimeUnit());
        if (Double.isFinite(value)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(writeDocument(gauge, getConsumer(new StringBuilder().append(value))));
            return writeReturn(list);
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeFunctionTimer(FunctionTimer timer) {
        double sum = timer.totalTime(getBaseTimeUnit());
        double mean = timer.mean(getBaseTimeUnit());
        if (Double.isFinite(sum) && Double.isFinite(mean)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(writeDocument(timer, getConsumer(new StringBuilder().append(timer.count()))));
            list.add(writeDocument(timer, "sum", getConsumer(new StringBuilder().append(sum))));
            list.add(writeDocument(timer, "mean", getConsumer(new StringBuilder().append(mean))));
            return writeReturn(list);
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeLongTaskTimer(LongTaskTimer timer) {
        ArrayList<String> list = new ArrayList<>();
        list.add(writeDocument(timer, "active.tasks", getConsumer(new StringBuilder().append(timer.activeTasks()))));
        list.add(writeDocument(timer, "duration", getConsumer(new StringBuilder().append(timer.duration(getBaseTimeUnit())))));
        return writeReturn(list);
    }

    // VisibleForTesting
    Optional<String> writeTimer(Timer timer) {
        ArrayList<String> list = new ArrayList<>();
        list.add(writeDocument(timer, getConsumer(new StringBuilder().append(timer.count()))));
        list.add(writeDocument(timer, "sum", getConsumer(new StringBuilder().append(timer.totalTime(getBaseTimeUnit())))));
        list.add(writeDocument(timer, "mean", getConsumer(new StringBuilder().append(timer.mean(getBaseTimeUnit())))));
        list.add(writeDocument(timer, "max", getConsumer(new StringBuilder().append(timer.max(getBaseTimeUnit())))));
        return writeReturn(list);

    }

    // VisibleForTesting
    Optional<String> writeSummary(DistributionSummary summary) {
        HistogramSnapshot histogramSnapshot = summary.takeSnapshot();

        ArrayList<String> list = new ArrayList<>();
        list.add(writeDocument(summary, getConsumer(new StringBuilder().append(histogramSnapshot.count()))));
        list.add(writeDocument(summary, "sum", getConsumer(new StringBuilder().append(histogramSnapshot.total()))));
        list.add(writeDocument(summary, "mean", getConsumer(new StringBuilder().append(histogramSnapshot.mean()))));
        list.add(writeDocument(summary, "max", getConsumer(new StringBuilder().append(histogramSnapshot.max()))));
        return writeReturn(list);
    }

    // VisibleForTesting
    Optional<String> writeMeter(Meter meter) {
        Iterable<Measurement> measurements = meter.measure();

        ArrayList<String> list = new ArrayList<>();

        for (Measurement measurement : measurements) {
            double value = measurement.getValue();
            if (!Double.isFinite(value)) {
                continue;
            }
            list.add(writeDocument(meter, measurement.getStatistic().getTagValueRepresentation(), getConsumer(new StringBuilder().append(value))));
        }

        return writeReturn(list);
    }

    Optional<String> writeReturn(ArrayList<String> list){
        list.removeIf(val -> val.length() == 0);
        String documents = Arrays.stream(list.toArray(new String[list.size()])).collect(Collectors.joining(",\n"));
        return documents.length() != 0 ?  Optional.of(documents) : Optional.empty();
    }

    String writeDocument(Meter meter, Consumer<StringBuilder> consumer){
        return writeDocument(meter, "", consumer);
    }

    String writeDocument(Meter meter, String name, Consumer<StringBuilder> consumer) {
        StringBuilder sb = new StringBuilder();
        name = getConventionName(meter.getId()) + (name.length() == 0 ? "" : "." + name);

        //不需要上传的metric
        if (config.metricBlockList() != null && config.metricBlockList().contains(name)){
            logger.debug("{} metric blocked.", name);
            return "";
        }
        //方法主体
        writeMessage(sb, name, meter);

        //用户回调
        consumer.accept(sb);
        sb.append("}");

        return sb.toString();
    }

    void writeMessage(StringBuilder sb, String name, Meter meter){
        Long timestamp = generateTimestamp();

        String type = meter.getId().getType().toString().toUpperCase();

        if (!type.equals("COUNTER") && !type.equals("GAUGE")){
            type = "GAUGE";
        }

        sb.append("{\"").append(config.timestampFieldName()).append("\":").append(timestamp)
                .append(",\"metric\":\"").append(escapeJson(name)).append('"')
                .append(",\"counterType\":\"").append(type).append('"')
                .append(",\"step\":").append(config.step().toMillis() / 1000);

        if (config.nid().length() != 0){
            sb.append(",\"nid\":\"").append(config.nid()).append('"');
        }else {
            sb.append(",\"endpoint\":\"").append(endpoint).append('"');
        }

        writeTags(sb, meter);
    }

    void writeTags(StringBuilder sb, Meter meter){
        List<Tag> tags = getConventionTags(meter.getId());

        if (!tags.isEmpty()){
            boolean flag = false;
            sb.append(",\"tags\":\"");
            if (config.appendTags().length() != 0){
                sb.append(config.appendTags());
                flag = true;
            }

            for (Tag tag : tags) {
                if (flag){
                    sb.append(",");
                }
                sb.append(escapeJson(tag.getKey())).append("=")
                        .append(escapeJson(tag.getValue().replace(" ", "-")));
                flag = true;
            }
            sb.append('"');

        }else if (config.appendTags().length() != 0){
            sb.append(",\"tags\":\"").append(config.appendTags()).append('"');
        }
    }

    @Override
    @NonNull
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}