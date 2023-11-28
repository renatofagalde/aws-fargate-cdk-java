package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class Service02Stack extends Stack {

    private final String PRODUCT_EVENTS = "PRODUCT-EVENTS";
//    private SnsTopic eventTopic;
//    private Table productEventTable;

    public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic eventTopic, Table productEventTable) {
        this(scope, id, null, cluster, eventTopic, productEventTable);
//        this.productEventTable = productEventTable;
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster,
                          SnsTopic eventTopic, Table productEventTable) {
        super(scope, id, props);

        Queue productEventDLQ = Queue.Builder.create(this, PRODUCT_EVENTS.concat("-DLQ"))
                .queueName(PRODUCT_EVENTS.concat("-DLQ"))
                .build();

        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(productEventDLQ)
                .maxReceiveCount(3) //mensagem tratada para redirecionar para a fila
                .build();

        final Queue productEventQueue = Queue.Builder.create(this, PRODUCT_EVENTS)
                .queueName(PRODUCT_EVENTS)
                .deadLetterQueue(deadLetterQueue)
                .build();

        //inscrerver minha fila no topico ** IMPORTANTE
        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventQueue).build();
        eventTopic.getTopic().addSubscription(sqsSubscription);

        ApplicationLoadBalancedFargateService service =
                ApplicationLoadBalancedFargateService.Builder
                        .create(this, "ALB02")
                        .serviceName("SERVICE-02")
                        .cluster(cluster)
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .desiredCount(2) //instances
                        .listenerPort(9090)
                        .taskImageOptions(this.createTask(productEventQueue))
                        .publicLoadBalancer(Boolean.TRUE)
                        .build();
        //health check
        service.getTargetGroup()
                .configureHealthCheck(HealthCheck.builder()
                        .path("/actuator/health")
                        .port("9090")
                        .healthyHttpCodes("200")
                        .build());

        final ScalableTaskCount scalableTaskCount = service.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2) // quem tem 1, nao tem nenhum, por isso 2
                .maxCapacity(4) // cuidado com o número de conexoes com o banco
                .build());

        //regras do auto-scaling
        scalableTaskCount.scaleOnCpuUtilization("SERVICE-02-AUTO-SCALING", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        productEventQueue.grantConsumeMessages(service.getTaskDefinition().getTaskRole());
        productEventTable.grantReadWriteData(service.getTaskDefinition().getTaskRole());

        CfnOutput.Builder.create(this, "SERVICE-02-ALB-URL")
                .exportName("SERVICE-02-ALB-URL")
                .value(service.getLoadBalancer().getLoadBalancerDnsName()) //objeto criado no comeco
                .build();
    }

    private Map createEnviroment(Queue productEventQueue) {

        final HashMap<Object, Object> enviromentVariables = new HashMap<>();
        enviromentVariables.put("AWS_REGION", "us-east-1");
        enviromentVariables.put("AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventQueue.getQueueName());

        return enviromentVariables;
    }

    private ApplicationLoadBalancedTaskImageOptions createTask(Queue productEventQueue) {
        return ApplicationLoadBalancedTaskImageOptions
                .builder()
                .containerName("AWS-PROJECT-02")
                .image(ContainerImage.fromRegistry("renatofagalde/aws-fargate-02:1.5.2"))
                .containerPort(9090)
                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(createLogGroup())
                        .streamPrefix("SERVICE-02")
                        .build()))
                .environment(this.createEnviroment(productEventQueue))
                .build();
    }

    @NotNull
    private LogGroup createLogGroup() {
        return LogGroup.Builder.create(this, "SERVICE-02-LOG-GROUP")
                .logGroupName("SERVICE-02")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }
}
