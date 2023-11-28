package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;


public class Service01Stack extends Stack {

    private SnsTopic eventTopic;
    private Bucket bucketFile;
    private Queue queueFile;

    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic eventTopic,
                          Bucket bucketFile, Queue queueFile) {
        this(scope, id, null, cluster, eventTopic, bucketFile, queueFile);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster,
                          SnsTopic eventTopic,
                          Bucket bucketFile, Queue queueFile) {
        super(scope, id, props);
        this.eventTopic = eventTopic;
        this.bucketFile = bucketFile;
        this.queueFile = queueFile;

        ApplicationLoadBalancedFargateService service01 =
                ApplicationLoadBalancedFargateService.Builder
                        .create(this, "ALB01")
                        .serviceName("SERVICE-01")
                        .cluster(cluster)
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .desiredCount(2) //instances
                        .listenerPort(8080)
                        .taskImageOptions(this.createTask())
                        .publicLoadBalancer(Boolean.TRUE)
                        .build();
        //health check
        service01.getTargetGroup()
                .configureHealthCheck(HealthCheck.builder()
                        .path("/actuator/health")
                        .port("8080")
                        .healthyHttpCodes("200")
                        .build());

        final ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2) // quem tem 1, nao tem nenhum, por isso 2
                .maxCapacity(4) // cuidado com o número de conexoes com o banco
                .build());

        //regras do auto-scaling
        scalableTaskCount.scaleOnCpuUtilization("SERVICE-01-AUTO-SCALING", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        //para quem dar a permissao
        eventTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());

        //permissao na fila
        this.queueFile.grantConsumeMessages(service01.getTaskDefinition().getTaskRole());
        this.bucketFile.grantReadWrite(service01.getTaskDefinition().getTaskRole());

        CfnOutput.Builder.create(this, "SERVICE-01-ALB-URL")
                .exportName("SERVICE-01-ALB-URL")
                .value(service01.getLoadBalancer().getLoadBalancerDnsName()) //objeto criado no comeco
                .build();
    }

    private Map createEnviroment() {
        final String SPRING_DATASOURCE_URL =
                String.format("jdbc:mysql://%s:3306/aws_project_01?createDatabaseIfNotExist=true",
                        Fn.importValue("RDS-ENDPOINT"));

        final HashMap<Object, Object> enviromentVariables = new HashMap<>();
        enviromentVariables.put("SPRING_DATASOURCE_URL", SPRING_DATASOURCE_URL);
        enviromentVariables.put("SPRING_DATASOURCE_USERNAME", "admin");
        //RDS-PASSWORD -> definida no output do RDS
        enviromentVariables.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("RDS-PASSWORD"));
        enviromentVariables.put("AWS_REGION", "us-east-1");
        enviromentVariables.put("AWS_SNS_TOPIC_PRODUCT_EVENTS_ARN", this.eventTopic.getTopic().getTopicArn());
        enviromentVariables.put("AWS_S3_BUCKET_FILE_NAME", this.bucketFile.getBucketName());
        enviromentVariables.put("AWS_SQS_QUEUE_FILE_EVENTS_NAME", this.queueFile.getQueueName());

        return enviromentVariables;
    }

    private ApplicationLoadBalancedTaskImageOptions createTask() {
        return ApplicationLoadBalancedTaskImageOptions
                .builder()
                .containerName("AWS-PROJECT-01")
                .image(ContainerImage.fromRegistry("renatofagalde/aws-fargate-01:1.6.4"))
                .containerPort(8080)
                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(createLogGroup())
                        .streamPrefix("SERVICE-01")
                        .build()))
                .environment(this.createEnviroment())
                .build();
    }

    @NotNull
    private LogGroup createLogGroup() {
        return LogGroup.Builder.create(this, "SERVICE-01-LOG-GROUP")
                .logGroupName("SERVICE-01")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }
}
