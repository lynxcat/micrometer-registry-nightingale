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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.micrometer.core.instrument.util.StringEscapeUtils.escapeJson;
import static java.util.stream.Collectors.joining;


public class NightingaleMeterRegistry extends StepMeterRegistry {

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("ngihtingale-metrics-publisher");
    private static final String SUCCESS_RESPONSE_BODY_SIGNATURE = "{\"err\":\"\"}";
    private final com.lynxcat.NightingaleConfig config;
    private final HttpSender httpClient;
    private final Logger logger = LoggerFactory.getLogger(NightingaleMeterRegistry.class);

    public NightingaleMeterRegistry(com.lynxcat.NightingaleConfig config, Clock clock) {
        this(config, clock, DEFAULT_THREAD_FACTORY,
                new HttpUrlConnectionSender(config.connectTimeout(), config.readTimeout()));
    }

    public NightingaleMeterRegistry(com.lynxcat.NightingaleConfig config, Clock clock, ThreadFactory threadFactory, HttpSender httpClient){
        super(config, clock);
        this.config = config;
        this.httpClient = httpClient;
        config().namingConvention(NamingConvention.dot);
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

                httpClient
                        .post(config.addr())
                        .withJsonContent(requestBody)
                        .send()
                        .onSuccess(response -> {
                            String responseBody = response.body();
                            if (responseBody.contains(SUCCESS_RESPONSE_BODY_SIGNATURE)){
                                logger.debug("successfully sent {} metrics to nightingale", response.body());
                            }else {
                                logger.debug("failed metrics payload: {}", requestBody);
                                logger.error("failed to send metrics to nightingale: {}", response.body());
                            }
                        })
                        .onError(response -> {
                            logger.debug("failed metrics payload: {}", requestBody);
                            logger.error("failed to send metrics to nightingale: {}", response.body());
                        });

            } catch (Throwable e) {
                logger.error("failed to send metrics to nightingale: {}", e.getMessage());
            }
        }
    }


    // VisibleForTesting
    Optional<String> writeCounter(Counter counter) {
        return writeCounter(counter, counter.count());
    }

    // VisibleForTesting
    Optional<String> writeFunctionCounter(FunctionCounter counter) {
        return writeCounter(counter, counter.count());
    }

    private Optional<String> writeCounter(Meter meter, double value) {
        if (Double.isFinite(value)) {
            return Optional.of(writeDocument(meter, builder -> {
                builder.append(",\"count\":").append(value);
            }));
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeGauge(Gauge gauge) {
        double value = gauge.value();
        if (Double.isFinite(value)) {
            return Optional.of(writeDocument(gauge, builder -> {
                builder.append(",\"value\":").append(value);
            }));
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeTimeGauge(TimeGauge gauge) {
        double value = gauge.value(getBaseTimeUnit());
        if (Double.isFinite(value)) {
            return Optional.of(writeDocument(gauge, builder -> {
                builder.append(",\"value\":").append(value);
            }));
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeFunctionTimer(FunctionTimer timer) {
        double sum = timer.totalTime(getBaseTimeUnit());
        double mean = timer.mean(getBaseTimeUnit());
        if (Double.isFinite(sum) && Double.isFinite(mean)) {
            return Optional.of(writeDocument(timer, builder -> {
                builder.append(",\"count\":").append(timer.count());
                builder.append(",\"sum\":").append(sum);
                builder.append(",\"mean\":").append(mean);
            }));
        }
        return Optional.empty();
    }

    // VisibleForTesting
    Optional<String> writeLongTaskTimer(LongTaskTimer timer) {
        return Optional.of(writeDocument(timer, builder -> {
            builder.append(",\"activeTasks\":").append(timer.activeTasks());
            builder.append(",\"duration\":").append(timer.duration(getBaseTimeUnit()));
        }));
    }

    // VisibleForTesting
    Optional<String> writeTimer(Timer timer) {
        return Optional.of(writeDocument(timer, builder -> {
            builder.append(",\"count\":").append(timer.count());
            builder.append(",\"sum\":").append(timer.totalTime(getBaseTimeUnit()));
            builder.append(",\"mean\":").append(timer.mean(getBaseTimeUnit()));
            builder.append(",\"max\":").append(timer.max(getBaseTimeUnit()));
        }));
    }

    // VisibleForTesting
    Optional<String> writeSummary(DistributionSummary summary) {
        HistogramSnapshot histogramSnapshot = summary.takeSnapshot();
        return Optional.of(writeDocument(summary, builder -> {
            builder.append(",\"count\":").append(histogramSnapshot.count());
            builder.append(",\"sum\":").append(histogramSnapshot.total());
            builder.append(",\"mean\":").append(histogramSnapshot.mean());
            builder.append(",\"max\":").append(histogramSnapshot.max());
        }));
    }

    // VisibleForTesting
    Optional<String> writeMeter(Meter meter) {
        Iterable<Measurement> measurements = meter.measure();
        List<String> names = new ArrayList<>();
        // Snapshot values should be used throughout this method as there are chances for values to be changed in-between.
        List<Double> values = new ArrayList<>();
        for (Measurement measurement : measurements) {
            double value = measurement.getValue();
            if (!Double.isFinite(value)) {
                continue;
            }
            names.add(measurement.getStatistic().getTagValueRepresentation());
            values.add(value);
        }
        if (names.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(writeDocument(meter, builder -> {
            for (int i = 0; i < names.size(); i++) {
                builder.append(",\"").append(names.get(i)).append("\":\"").append(values.get(i)).append("\"");
            }
        }));
    }

    protected Long generateTimestamp() {
        return config().clock().wallTime() / 1000;
    }

    String writeDocument(Meter meter, Consumer<StringBuilder> consumer) {
        StringBuilder sb = new StringBuilder();
        Long timestamp = generateTimestamp();
        String name = getConventionName(meter.getId());
        String type = meter.getId().getType().toString().toUpperCase();


        sb.append("{\"").append(config.timestampFieldName()).append("\":").append(timestamp)
                .append(",\"metric\":\"").append(escapeJson(name)).append('"')
                .append(",\"counterType\":\"").append(type).append('"')
                .append(",\"step\":").append(config.step().toMillis() / 1000);

        if (config.nid() != ""){
            sb.append(",\"nid\":\"").append(config.nid()).append('"');
        }else {
            sb.append(",\"endpoint\":\"").append(config.endpoint()).append('"');
        }

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

        consumer.accept(sb);
        sb.append("}");

        return sb.toString();
    }



    @Override
    @NonNull
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}