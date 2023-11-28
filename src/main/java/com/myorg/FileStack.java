package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.notifications.SnsDestination;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;


public class FileStack extends Stack {

    private Bucket bucket;
    private Queue fileQueue;

    public FileStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public FileStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final SnsTopic s3FileSNSTopic = SnsTopic.Builder
                .create(Topic.Builder.create(this, "S3FILETOPIC")
                        .topicName("S3-FILES-EVENTS")
                        .build())
                .build();

        this.bucket = Bucket.Builder.create(this, "S301")
                .bucketName("likwi-bucket-s3-files")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new SnsDestination(s3FileSNSTopic.getTopic()));

        Queue s3FileSQSDLQ = Queue.Builder
                .create(this, "s3FILESSQSDLQ")
                .queueName("S3-FILES-EVENTS-DLQ")
                .build();

        final DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(s3FileSQSDLQ)
                .maxReceiveCount(3)
                .build();

        fileQueue = Queue.Builder.create(this, "s3FILESSQS")
                .queueName("S3-FILES-EVENTS")
                .deadLetterQueue(deadLetterQueue)
                .build();

        final SqsSubscription sqsSubscription = SqsSubscription.Builder.create(fileQueue).build();
        s3FileSNSTopic.getTopic().addSubscription(sqsSubscription);

    }

    public Bucket getBucket() {
        return bucket;
    }

    public Queue getFileQueue() {
        return fileQueue;
    }
}
